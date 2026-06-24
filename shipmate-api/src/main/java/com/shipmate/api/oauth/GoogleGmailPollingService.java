package com.shipmate.api.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.aiextraction.AiExtractionService;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.config.GoogleOAuthProperties;
import com.shipmate.api.emailorder.EmailAttachmentService;
import com.shipmate.api.emailorder.EmailMimeDecoder;
import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.inboundmessage.CreateInboundMessageRequest;
import com.shipmate.api.inboundmessage.InboundMessageResponse;
import com.shipmate.api.inboundmessage.InboundMessageService;
import com.shipmate.api.inboundmessage.SourceChannel;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.operation.error.ErrorLogService;
import com.shipmate.api.orderdraft.CreateOrderDraftFromInboundMessageRequest;
import com.shipmate.api.orderdraft.OrderDraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleGmailPollingService {
    private static final Logger log = LoggerFactory.getLogger(GoogleGmailPollingService.class);
    private static final String GMAIL_MESSAGES_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private final GoogleOAuthProperties properties;
    private final GoogleOAuthRepository repository;
    private final GoogleGmailPollingRunRepository runRepository;
    private final InboundMessageService inboundMessageService;
    private final OrderDraftService orderDraftService;
    private final AiExtractionService aiExtractionService;
    private final EmailAttachmentService emailAttachmentService;
    private final AuditLogService auditLogService;
    private final ErrorLogService errorLogService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GoogleGmailPollingService(
            GoogleOAuthProperties properties,
            GoogleOAuthRepository repository,
            GoogleGmailPollingRunRepository runRepository,
            InboundMessageService inboundMessageService,
            OrderDraftService orderDraftService,
            AiExtractionService aiExtractionService,
            EmailAttachmentService emailAttachmentService,
            AuditLogService auditLogService,
            ErrorLogService errorLogService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.repository = repository;
        this.runRepository = runRepository;
        this.inboundMessageService = inboundMessageService;
        this.orderDraftService = orderDraftService;
        this.aiExtractionService = aiExtractionService;
        this.emailAttachmentService = emailAttachmentService;
        this.auditLogService = auditLogService;
        this.errorLogService = errorLogService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${shipmate.google-oauth.gmail-polling-delay-ms:300000}")
    public void collectAllConnectedAccountsOnSchedule() {
        if (!properties.gmailPollingEnabled()) {
            return;
        }
        try {
            GoogleGmailPollingResponse response = collectAllConnectedAccounts("SCHEDULE");
            if (response.collectedCount() > 0) {
                log.info("GOOGLE_GMAIL_POLLING_COLLECTED collectedCount={} scannedCount={}", response.collectedCount(), response.scannedCount());
            }
        } catch (Exception exception) {
            errorLogService.append(
                    "SM-GML-9101",
                    HttpStatus.BAD_GATEWAY.value(),
                    "Gmail polling 실행에 실패했습니다: " + exception.getMessage(),
                    exception
            );
        }
    }

    public GoogleGmailPollingResponse collectAllConnectedAccounts() {
        return collectAllConnectedAccounts("MANUAL_ALL");
    }

    private GoogleGmailPollingResponse collectAllConnectedAccounts(String triggerType) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        try {
            GoogleGmailPollingResponse response = collectAllConnectedAccountsInternal();
            saveRun(triggerType, null, response, true, null, null, startedAt);
            return response;
        } catch (Exception exception) {
            saveRun(triggerType, null, emptyResponse(), false, failureCode(exception), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    private GoogleGmailPollingResponse collectAllConnectedAccountsInternal() {
        List<GoogleOAuthToken> tokens = repository.findAllTokens();
        int scannedCount = 0;
        int collectedCount = 0;
        int orderDraftCreatedCount = 0;
        int attachmentStoredCount = 0;
        int skippedDuplicateCount = 0;
        int skippedNonOrderCount = 0;
        for (GoogleOAuthToken token : tokens) {
            CollectionResult result = collectForToken(token);
            scannedCount += result.scannedCount();
            collectedCount += result.collectedCount();
            orderDraftCreatedCount += result.orderDraftCreatedCount();
            attachmentStoredCount += result.attachmentStoredCount();
            skippedDuplicateCount += result.skippedDuplicateCount();
            skippedNonOrderCount += result.skippedNonOrderCount();
        }
        return new GoogleGmailPollingResponse(
                tokens.size(),
                scannedCount,
                collectedCount,
                orderDraftCreatedCount,
                attachmentStoredCount,
                skippedDuplicateCount,
                skippedNonOrderCount,
                "Gmail OAuth polling 완료"
        );
    }

    public GoogleGmailPollingResponse collectMerchant(String merchantId) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        try {
            GoogleOAuthToken token = repository.findTokenByMerchantId(merchantId)
                    .orElseThrow(() -> new ApiRequestException("SM-GML-9102", HttpStatus.NOT_FOUND, "Google OAuth 연결 토큰이 없습니다."));
            CollectionResult result = collectForToken(token);
            GoogleGmailPollingResponse response = new GoogleGmailPollingResponse(
                    1,
                    result.scannedCount(),
                    result.collectedCount(),
                    result.orderDraftCreatedCount(),
                    result.attachmentStoredCount(),
                    result.skippedDuplicateCount(),
                    result.skippedNonOrderCount(),
                    "Gmail OAuth polling 완료"
            );
            saveRun("MANUAL_MERCHANT", merchantId, response, true, null, null, startedAt);
            return response;
        } catch (Exception exception) {
            saveRun("MANUAL_MERCHANT", merchantId, emptyResponse(), false, failureCode(exception), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    public GoogleGmailPollingStatusResponse status() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(7);
        GoogleGmailPollingRun latestFailure = runRepository.latestFailure().orElse(null);
        return new GoogleGmailPollingStatusResponse(
                properties.gmailPollingEnabled(),
                properties.gmailPollingDelayMs(),
                properties.gmailPollingMaxMessages(),
                properties.gmailAutoCreateOrderDrafts(),
                repository.countTokens(),
                repository.countCollectedMessages(),
                repository.latestCollectedAt().orElse(null),
                repository.latestReceivedAt().orElse(null),
                repository.countStoredAttachments(),
                runRepository.countSince(from),
                runRepository.countFailureSince(from),
                runRepository.sumSkippedNonOrderSince(from),
                runRepository.latestSuccess().map(GoogleGmailPollingRun::finishedAt).orElse(null),
                latestFailure == null ? null : latestFailure.finishedAt(),
                latestFailure == null ? null : latestFailure.failureCode(),
                latestFailure == null ? null : latestFailure.failureMessage(),
                latestFailure == null ? null : latestFailure.latestInboundMessageId(),
                runRepository.failureCodeCountsSince(from)
        );
    }

    public GoogleGmailMarkReadResponse markLatestCollectedMessagesRead(String merchantId, Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 2 : Math.min(limit, 20);
        GoogleOAuthToken token = repository.findTokenByMerchantId(merchantId)
                .orElseThrow(() -> new ApiRequestException("SM-GML-9102", HttpStatus.NOT_FOUND, "Google OAuth 연결 토큰이 없습니다."));
        GoogleOAuthToken usableToken = refreshIfNeeded(token);
        List<String> messageIds = repository.findLatestCollectedGmailMessageIds(merchantId, normalizedLimit);
        int markedReadCount = 0;
        for (String messageId : messageIds) {
            markRead(messageId, usableToken);
            markedReadCount++;
        }
        return new GoogleGmailMarkReadResponse(
                merchantId,
                messageIds.size(),
                markedReadCount,
                "Gmail 수집 이력 최신 " + messageIds.size() + "건 읽음 처리 완료"
        );
    }

    public List<GoogleGmailPollingRun> recentRuns(Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return runRepository.search(new GoogleGmailPollingRunSearchCriteria(null, null, null, normalizedLimit));
    }

    public List<GoogleGmailPollingRun> searchRuns(String merchantId, Boolean success, Integer days, Integer limit) {
        int normalizedLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return runRepository.search(new GoogleGmailPollingRunSearchCriteria(
                merchantId == null || merchantId.isBlank() ? null : merchantId.strip(),
                success,
                days,
                normalizedLimit
        ));
    }

    private CollectionResult collectForToken(GoogleOAuthToken token) {
        GoogleOAuthToken usableToken = refreshIfNeeded(token);
        JsonNode listResponse = sendJson(authorizedGet(messageListUrl(), usableToken.accessToken()), "Gmail 메시지 목록을 조회하지 못했습니다.");
        int scannedCount = 0;
        int collectedCount = 0;
        int orderDraftCreatedCount = 0;
        int attachmentStoredCount = 0;
        int skippedDuplicateCount = 0;
        int skippedNonOrderCount = 0;
        for (JsonNode messageRef : listResponse.path("messages")) {
            scannedCount++;
            String messageId = messageRef.path("id").asText("");
            if (messageId.isBlank()) {
                continue;
            }
            if (repository.isGmailMessageCollected(usableToken.merchantId(), messageId)) {
                skippedDuplicateCount++;
                continue;
            }
            JsonNode message = sendJson(authorizedGet(messageDetailUrl(messageId), usableToken.accessToken()), "Gmail 메시지를 조회하지 못했습니다.");
            CollectedMessage collectedMessage = toCollectedMessage(message);
            if (!looksLikeOrderEmail(collectedMessage)) {
                markReadIfPossible(messageId, usableToken);
                skippedNonOrderCount++;
                continue;
            }
            InboundMessageResponse inbound = inboundMessageService.create(new CreateInboundMessageRequest(
                    usableToken.merchantId(),
                    null,
                    null,
                    SourceChannel.EMAIL,
                    BusinessChannel.PACKAGING_CUSTOM,
                    collectedMessage.rawText(),
                    collectedMessage.receivedAt()
            ));
            repository.saveCollectedGmailMessage(
                    usableToken.merchantId(),
                    collectedMessage.googleMessageId(),
                    collectedMessage.googleThreadId(),
                    inbound.inboundMessageId(),
                    collectedMessage.subject(),
                    collectedMessage.fromEmail(),
                    collectedMessage.receivedAt(),
                    OffsetDateTime.now()
            );
            if (properties.gmailAutoCreateOrderDrafts() && createOrderDraft(inbound, usableToken)) {
                orderDraftCreatedCount++;
            }
            attachmentStoredCount += storeAttachments(messageId, collectedMessage, inbound, usableToken);
            markReadIfPossible(messageId, usableToken);
            collectedCount++;
        }
        if (collectedCount > 0) {
            auditLogService.append(
                    usableToken.merchantId(),
                    null,
                    "GOOGLE_GMAIL_POLLED",
                    "GOOGLE_GMAIL",
                    usableToken.googleEmail(),
                    "Gmail OAuth polling으로 주문 원문 " + collectedCount + "건을 수집했습니다."
            );
        }
        return new CollectionResult(scannedCount, collectedCount, orderDraftCreatedCount, attachmentStoredCount, skippedDuplicateCount, skippedNonOrderCount);
    }

    private boolean createOrderDraft(InboundMessageResponse inbound, GoogleOAuthToken token) {
        try {
            var orderDraft = orderDraftService.createFromInboundMessage(new CreateOrderDraftFromInboundMessageRequest(inbound.inboundMessageId()));
            aiExtractionService.extractAndApply(orderDraft.orderDraftId(), true);
            return true;
        } catch (Exception exception) {
            errorLogService.append(
                    "SM-GML-9106",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Gmail 수집 원문에서 출고표 초안을 자동 생성하지 못했습니다. inboundMessageId=" + inbound.inboundMessageId()
                            + ", merchantId=" + token.merchantId(),
                    exception
            );
            return false;
        }
    }

    private int storeAttachments(String messageId, CollectedMessage message, InboundMessageResponse inbound, GoogleOAuthToken token) {
        int storedCount = 0;
        for (AttachmentCandidate attachment : message.attachments()) {
            if (attachment.attachmentId().isBlank()) {
                continue;
            }
            try {
                JsonNode attachmentResponse = sendJson(
                        authorizedGet(messageAttachmentUrl(messageId, attachment.attachmentId()), token.accessToken()),
                        "Gmail 첨부파일을 조회하지 못했습니다."
                );
                byte[] bytes = decodeBytes(attachmentResponse.path("data").asText(""));
                if (bytes.length == 0) {
                    continue;
                }
                emailAttachmentService.save(
                        token.merchantId(),
                        inbound.inboundMessageId(),
                        SourceChannel.EMAIL,
                        message.googleMessageId(),
                        attachment.fileName(),
                        attachment.contentType(),
                        bytes
                );
                storedCount++;
            } catch (Exception exception) {
                errorLogService.append(
                        "SM-GML-9107",
                        HttpStatus.BAD_GATEWAY.value(),
                        "Gmail 첨부 원본을 저장하지 못했습니다. inboundMessageId=" + inbound.inboundMessageId()
                                + ", googleMessageId=" + message.googleMessageId()
                                + ", fileName=" + attachment.fileName(),
                        exception
                );
            }
        }
        return storedCount;
    }

    private GoogleOAuthToken refreshIfNeeded(GoogleOAuthToken token) {
        if (token.expiresAt() == null || token.expiresAt().isAfter(OffsetDateTime.now().plusMinutes(1))) {
            return token;
        }
        if (token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new ApiRequestException("SM-GML-9103", HttpStatus.BAD_GATEWAY, "Google refresh token이 없어 Gmail 토큰을 갱신할 수 없습니다.");
        }
        String body = "client_id=" + encode(properties.clientId())
                + "&client_secret=" + encode(properties.clientSecret())
                + "&refresh_token=" + encode(token.refreshToken())
                + "&grant_type=refresh_token";
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        JsonNode refreshed = sendJson(request, "Google OAuth access token 갱신에 실패했습니다.");
        String accessToken = refreshed.path("access_token").asText("");
        int expiresIn = refreshed.path("expires_in").asInt(0);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = expiresIn > 0 ? now.plusSeconds(expiresIn) : null;
        repository.updateAccessToken(token.merchantId(), accessToken, expiresAt, now);
        return new GoogleOAuthToken(
                token.merchantId(),
                token.googleEmail(),
                accessToken,
                token.refreshToken(),
                token.scope(),
                refreshed.path("token_type").asText(token.tokenType()),
                expiresAt,
                now
        );
    }

    private HttpRequest authorizedGet(String url, String accessToken) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
    }

    private String messageListUrl() {
        int maxResults = properties.gmailPollingMaxMessages() < 1 ? 10 : Math.min(properties.gmailPollingMaxMessages(), 50);
        return GMAIL_MESSAGES_ENDPOINT
                + "?maxResults=" + maxResults
                + "&q=" + encode("in:inbox is:unread newer_than:14d");
    }

    private String messageDetailUrl(String messageId) {
        return GMAIL_MESSAGES_ENDPOINT + "/" + encode(messageId) + "?format=full";
    }

    private String messageAttachmentUrl(String messageId, String attachmentId) {
        return GMAIL_MESSAGES_ENDPOINT + "/" + encode(messageId) + "/attachments/" + encode(attachmentId);
    }

    private void markRead(String messageId, GoogleOAuthToken token) {
        String body = "{\"removeLabelIds\":[\"UNREAD\"]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(GMAIL_MESSAGES_ENDPOINT + "/" + encode(messageId) + "/modify"))
                .header("Authorization", "Bearer " + token.accessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        sendJson(request, "Gmail 메시지 읽음 처리에 실패했습니다.");
    }

    private void markReadIfPossible(String messageId, GoogleOAuthToken token) {
        try {
            markRead(messageId, token);
        } catch (Exception exception) {
            errorLogService.append(
                    "SM-GML-9108",
                    HttpStatus.BAD_GATEWAY.value(),
                    "Gmail 메시지 읽음 처리에 실패했습니다. 중복 수집은 DB 이력으로 방지합니다. merchantId=" + token.merchantId()
                            + ", googleMessageId=" + messageId,
                    exception
            );
        }
    }

    private CollectedMessage toCollectedMessage(JsonNode message) {
        String googleMessageId = message.path("id").asText("");
        String googleThreadId = message.path("threadId").asText(null);
        JsonNode payload = message.path("payload");
        String subject = header(payload, "Subject");
        String fromEmail = header(payload, "From");
        OffsetDateTime receivedAt = receivedAt(message);
        String body = firstTextBody(payload);
        String rawText = """
                보낸 사람: %s
                제목: %s

                %s
                """.formatted(valueOrBlank(fromEmail), valueOrBlank(subject), valueOrBlank(body)).strip();
        List<AttachmentCandidate> attachments = attachments(payload);
        if (!attachments.isEmpty()) {
            rawText = (rawText + "\n\n첨부파일 후보: " + attachments.stream()
                    .map(AttachmentCandidate::fileName)
                    .filter(value -> !value.isBlank())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("이름 없음")).strip();
        }
        return new CollectedMessage(googleMessageId, googleThreadId, subject, fromEmail, receivedAt, rawText, attachments);
    }

    private String header(JsonNode payload, String name) {
        for (JsonNode header : payload.path("headers")) {
            if (name.equalsIgnoreCase(header.path("name").asText())) {
                return EmailMimeDecoder.decodeHeader(header.path("value").asText(""));
            }
        }
        return "";
    }

    private OffsetDateTime receivedAt(JsonNode message) {
        String internalDate = message.path("internalDate").asText("");
        if (internalDate.matches("\\d+")) {
            return Instant.ofEpochMilli(Long.parseLong(internalDate)).atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.now();
    }

    private String firstTextBody(JsonNode payload) {
        String direct = decodeBody(payload.path("body").path("data").asText(""), payload.path("mimeType").asText(""));
        if (!direct.isBlank() && isTextPart(payload)) {
            return direct;
        }
        List<String> bodies = new ArrayList<>();
        collectTextBodies(payload, bodies);
        return bodies.stream()
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private void collectTextBodies(JsonNode node, List<String> bodies) {
        if (isTextPart(node)) {
            String body = decodeBody(node.path("body").path("data").asText(""), node.path("mimeType").asText(""));
            if (!body.isBlank()) {
                bodies.add(body);
            }
        }
        for (JsonNode part : node.path("parts")) {
            collectTextBodies(part, bodies);
        }
    }

    private boolean isTextPart(JsonNode node) {
        String mimeType = node.path("mimeType").asText("");
        return mimeType.equalsIgnoreCase("text/plain") || mimeType.equalsIgnoreCase("text/html");
    }

    private List<AttachmentCandidate> attachments(JsonNode payload) {
        List<AttachmentCandidate> attachments = new ArrayList<>();
        collectAttachments(payload, attachments);
        return attachments;
    }

    private void collectAttachments(JsonNode node, List<AttachmentCandidate> attachments) {
        String filename = node.path("filename").asText("");
        if (!filename.isBlank()) {
            attachments.add(new AttachmentCandidate(
                    EmailMimeDecoder.decodeParameterValue(filename),
                    node.path("mimeType").asText("application/octet-stream"),
                    node.path("body").path("attachmentId").asText("")
            ));
        }
        for (JsonNode part : node.path("parts")) {
            collectAttachments(part, attachments);
        }
    }

    private String decodeBody(String data, String contentType) {
        if (data == null || data.isBlank()) {
            return "";
        }
        try {
            return EmailMimeDecoder.normalizeBody(
                    new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8),
                    contentType
            );
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private byte[] decodeBytes(String data) {
        if (data == null || data.isBlank()) {
            return new byte[0];
        }
        try {
            return Base64.getUrlDecoder().decode(data);
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private JsonNode sendJson(HttpRequest request, String failureMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiRequestException("SM-GML-9104", HttpStatus.BAD_GATEWAY, failureMessage + " 상태: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (ApiRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiRequestException("SM-GML-9105", HttpStatus.BAD_GATEWAY, failureMessage);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private void saveRun(
            String triggerType,
            String merchantId,
            GoogleGmailPollingResponse response,
            boolean success,
            String failureCode,
            String failureMessage,
            OffsetDateTime startedAt
    ) {
        try {
            runRepository.save(new GoogleGmailPollingRun(
                    "gmlr_" + UUID.randomUUID().toString().replace("-", ""),
                    triggerType,
                    merchantId,
                    response.accountCount(),
                    response.scannedCount(),
                    response.collectedCount(),
                    response.orderDraftCreatedCount(),
                    response.attachmentStoredCount(),
                    response.skippedDuplicateCount(),
                    response.skippedNonOrderCount(),
                    success,
                    failureCode,
                    truncate(failureMessage, 1000),
                    repository.latestCollectedInboundMessageId(merchantId).orElse(null),
                    startedAt,
                    OffsetDateTime.now()
            ));
        } catch (Exception exception) {
            log.warn("GMAIL_POLLING_RUN_SAVE_FAILED triggerType={}", triggerType, exception);
        }
    }

    private GoogleGmailPollingResponse emptyResponse() {
        return new GoogleGmailPollingResponse(0, 0, 0, 0, 0, 0, 0, "Gmail OAuth polling 실패");
    }

    private String failureCode(Exception exception) {
        if (exception instanceof ApiRequestException apiRequestException) {
            return apiRequestException.code();
        }
        return "SM-GML-9101";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean looksLikeOrderEmail(CollectedMessage message) {
        String subjectAndSender = (valueOrBlank(message.subject()) + "\n"
                + valueOrBlank(message.fromEmail())).toLowerCase(Locale.ROOT);
        String haystack = (subjectAndSender + "\n" + valueOrBlank(message.rawText())).toLowerCase(Locale.ROOT);
        if (containsAny(subjectAndSender,
                "google", "보안 알림", "security alert", "비밀번호가 변경", "새로운 계정 로그인",
                "개인 정보 보호", "privacy", "verification code", "인증 코드", "no-reply@", "noreply@")) {
            return false;
        }
        return containsAny(haystack,
                "주문", "발주", "견적", "납기", "배송지", "수령인", "연락처",
                "수량", "박스", "포장", "인쇄", "규격", "택배", "order", "po ");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private record CollectionResult(
            int scannedCount,
            int collectedCount,
            int orderDraftCreatedCount,
            int attachmentStoredCount,
            int skippedDuplicateCount,
            int skippedNonOrderCount
    ) {
    }

    private record CollectedMessage(
            String googleMessageId,
            String googleThreadId,
            String subject,
            String fromEmail,
            OffsetDateTime receivedAt,
            String rawText,
            List<AttachmentCandidate> attachments
    ) {
    }

    private record AttachmentCandidate(
            String fileName,
            String contentType,
            String attachmentId
    ) {
    }
}
