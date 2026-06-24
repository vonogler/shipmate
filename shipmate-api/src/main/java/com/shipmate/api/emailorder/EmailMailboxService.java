package com.shipmate.api.emailorder;

import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.inboundmessage.CreateInboundMessageRequest;
import com.shipmate.api.inboundmessage.InboundMessageService;
import com.shipmate.api.inboundmessage.SourceChannel;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.operation.error.ErrorLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailMailboxService {
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 15000;
    private static final int FETCH_LIMIT = 5;
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("(?im)^Subject:\\s*(.+)$");
    private static final Pattern FROM_PATTERN = Pattern.compile("(?im)^From:\\s*(.+)$");
    private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("(?im)^Content-Type:\\s*([^;\\r\\n]+)");
    private static final Pattern TRANSFER_ENCODING_PATTERN = Pattern.compile("(?im)^Content-Transfer-Encoding:\\s*([^;\\r\\n]+)");
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("(?i)boundary=(?:\"([^\"]+)\"|([^;\\r\\n]+))");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("(?i)filename\\*?=(?:\"([^\"]+)\"|([^;\\r\\n]+))");

    private final EmailMailboxConfigRepository repository;
    private final InboundMessageService inboundMessageService;
    private final EmailAttachmentService emailAttachmentService;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;
    private final ErrorLogService errorLogService;

    public EmailMailboxService(
            EmailMailboxConfigRepository repository,
            InboundMessageService inboundMessageService,
            EmailAttachmentService emailAttachmentService,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService,
            ErrorLogService errorLogService
    ) {
        this.repository = repository;
        this.inboundMessageService = inboundMessageService;
        this.emailAttachmentService = emailAttachmentService;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
        this.errorLogService = errorLogService;
    }

    public EmailMailboxConfigResponse saveConfig(EmailMailboxConfigRequest request) {
        String merchantId = requestAuthContext.currentMerchantIdOr(request.merchantId().strip());
        EmailMailboxConfig config = repository.save(new EmailMailboxConfig(
                merchantId,
                request.host().strip(),
                request.port(),
                request.username().strip(),
                request.encryptedPasswordMemo().strip(),
                request.sslEnabled(),
                "설정 저장됨",
                OffsetDateTime.now()
        ));
        auditLogService.appendForCurrentActor(
                merchantId,
                null,
                "EMAIL_MAILBOX_CONFIG_SAVED",
                "EMAIL_MAILBOX",
                merchantId,
                "이메일 수집 설정이 저장되었습니다."
        );
        return config.toResponse("설정 저장됨");
    }

    public EmailMailboxConfigResponse collect(String merchantId) {
        String readableMerchantId = requestAuthContext.currentMerchantIdOr(merchantId);
        EmailMailboxConfig config = repository.findByMerchantId(readableMerchantId)
                .orElseThrow(() -> new ApiRequestException("SM-EML-7101", HttpStatus.NOT_FOUND, "이메일 수집 설정이 없습니다."));
        ProbeResult probeResult = collectMailbox(config, readableMerchantId);
        repository.updateCollectionStatus(readableMerchantId, probeResult.status(), OffsetDateTime.now());
        auditLogService.appendForCurrentActor(
                readableMerchantId,
                null,
                "EMAIL_MAILBOX_COLLECTED",
                "EMAIL_MAILBOX",
                readableMerchantId,
                probeResult.status()
        );
        if (!probeResult.success()) {
            errorLogService.append(
                    "SM-EML-7102",
                    HttpStatus.BAD_GATEWAY.value(),
                    "이메일 메일함 연결 확인에 실패했습니다: " + probeResult.status(),
                    probeResult.exception()
            );
        }
        return config.toResponse(probeResult.status());
    }

    private ProbeResult collectMailbox(EmailMailboxConfig config, String merchantId) {
        int port = config.port() > 0 ? config.port() : (config.sslEnabled() ? 993 : 143);
        try (Socket socket = config.sslEnabled()
                ? SSLSocketFactory.getDefault().createSocket()
                : new Socket()) {
            socket.connect(new InetSocketAddress(config.host(), port), CONNECT_TIMEOUT_MILLIS);
            socket.setSoTimeout(READ_TIMEOUT_MILLIS);

            ImapSession session = new ImapSession(socket);
            session.greeting();
            session.command("LOGIN " + quote(config.username()) + " " + quote(config.encryptedPasswordMemo()));
            session.command("SELECT INBOX");
            List<Integer> messageNumbers = recentMessageNumbers(session.command("SEARCH UNSEEN"));
            if (messageNumbers.isEmpty()) {
                return new ProbeResult(true, "IMAP 로그인 완료. 새로 수집할 읽지 않은 메일이 없습니다.", null);
            }
            int collectedCount = 0;
            int attachmentCount = 0;
            for (Integer messageNumber : messageNumbers) {
                String fetched = session.command("FETCH " + messageNumber + " (BODY.PEEK[])");
                String rawText = toInboundRawText(fetched);
                if (rawText.isBlank()) {
                    continue;
                }
                List<ImapAttachmentCandidate> attachments = attachments(fetched);
                attachmentCount += attachments.size();
                var inbound = inboundMessageService.create(new CreateInboundMessageRequest(
                        merchantId,
                        requestAuthContext.currentActorIdOr(null),
                        null,
                        SourceChannel.EMAIL,
                        BusinessChannel.PACKAGING_CUSTOM,
                        appendAttachmentSummary(rawText, attachments.stream().map(ImapAttachmentCandidate::fileName).toList()),
                        OffsetDateTime.now()
                ));
                storeAttachments(merchantId, inbound.inboundMessageId(), attachments);
                session.command("STORE " + messageNumber + " +FLAGS (\\Seen)");
                collectedCount++;
            }
            session.command("LOGOUT");
            return new ProbeResult(
                    true,
                    "IMAP 로그인 및 본문 수집 완료: " + collectedCount + "건, 첨부 후보 " + attachmentCount + "개",
                    null
            );
        } catch (Exception exception) {
            return new ProbeResult(false, "IMAP 수집 실패: " + exception.getMessage(), exception);
        }
    }

    private List<Integer> recentMessageNumbers(String searchResponse) {
        List<Integer> numbers = new ArrayList<>();
        for (String token : searchResponse.replace("* SEARCH", "").split("\\s+")) {
            if (token.matches("\\d+")) {
                numbers.add(Integer.parseInt(token));
            }
        }
        int fromIndex = Math.max(0, numbers.size() - FETCH_LIMIT);
        return numbers.subList(fromIndex, numbers.size());
    }

    private String toInboundRawText(String fetched) {
        String subject = EmailMimeDecoder.decodeHeader(firstMatch(SUBJECT_PATTERN, fetched));
        String from = EmailMimeDecoder.decodeHeader(firstMatch(FROM_PATTERN, fetched));
        String body = stripImapMetadata(fetched);
        return """
                보낸 사람: %s
                제목: %s

                %s
                """.formatted(from, subject, body).strip();
    }

    private String stripImapMetadata(String fetched) {
        return fetched.lines()
                .filter(line -> !line.startsWith("* "))
                .filter(line -> !line.matches("A\\d+ OK.*"))
                .filter(line -> !line.toLowerCase(Locale.ROOT).startsWith("content-transfer-encoding: base64"))
                .limit(300)
                .reduce("", (left, right) -> left + (left.isBlank() ? "" : "\n") + right)
                .strip();
    }

    private String appendAttachmentSummary(String rawText, List<String> attachmentNames) {
        if (attachmentNames.isEmpty()) {
            return rawText;
        }
        return (rawText + "\n\n첨부파일 후보: " + String.join(", ", attachmentNames)).strip();
    }

    private List<ImapAttachmentCandidate> attachments(String fetched) {
        List<ImapAttachmentCandidate> attachments = new ArrayList<>();
        String boundary = boundary(fetched);
        if (boundary.isBlank()) {
            return attachments;
        }
        String delimiter = "--" + boundary;
        for (String part : fetched.replace("\r\n", "\n").split(Pattern.quote(delimiter))) {
            String fileName = attachmentName(part);
            if (fileName.isBlank()) {
                continue;
            }
            String contentType = firstMatch(CONTENT_TYPE_PATTERN, part);
            String transferEncoding = firstMatch(TRANSFER_ENCODING_PATTERN, part);
            String body = partBody(part);
            byte[] bytes = EmailMimeDecoder.decodeTransfer(body, transferEncoding);
            if (bytes.length > 0) {
                attachments.add(new ImapAttachmentCandidate(
                        fileName,
                        contentType.isBlank() ? "application/octet-stream" : contentType.strip(),
                        bytes
                ));
            }
        }
        return attachments;
    }

    private void storeAttachments(String merchantId, String inboundMessageId, List<ImapAttachmentCandidate> attachments) {
        for (ImapAttachmentCandidate attachment : attachments) {
            try {
                emailAttachmentService.save(
                        merchantId,
                        inboundMessageId,
                        SourceChannel.EMAIL,
                        null,
                        attachment.fileName(),
                        attachment.contentType(),
                        attachment.bytes()
                );
            } catch (Exception exception) {
                errorLogService.append(
                        "SM-EML-7204",
                        HttpStatus.BAD_GATEWAY.value(),
                        "IMAP 첨부 원본을 저장하지 못했습니다. inboundMessageId=" + inboundMessageId
                                + ", fileName=" + attachment.fileName(),
                        exception
                );
            }
        }
    }

    private String boundary(String fetched) {
        Matcher matcher = BOUNDARY_PATTERN.matcher(fetched);
        if (!matcher.find()) {
            return "";
        }
        return valueOrBlank(matcher.group(1), matcher.group(2)).strip();
    }

    private String attachmentName(String part) {
        Matcher matcher = ATTACHMENT_PATTERN.matcher(part);
        if (!matcher.find()) {
            return "";
        }
        return EmailMimeDecoder.decodeParameterValue(valueOrBlank(matcher.group(1), matcher.group(2)));
    }

    private String partBody(String part) {
        int separatorIndex = part.indexOf("\n\n");
        if (separatorIndex < 0) {
            return "";
        }
        return part.substring(separatorIndex + 2).strip();
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).strip() : "";
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String valueOrBlank(String first, String second) {
        return first != null ? first : second == null ? "" : second;
    }

    private record ProbeResult(boolean success, String status, Exception exception) {
    }

    private record ImapAttachmentCandidate(String fileName, String contentType, byte[] bytes) {
    }

    private static class ImapSession {
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private int tagSequence = 1;

        ImapSession(Socket socket) throws Exception {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void greeting() throws Exception {
            String line = reader.readLine();
            if (line == null || !line.startsWith("* OK")) {
                throw new IllegalStateException("IMAP greeting 실패");
            }
        }

        String command(String command) throws Exception {
            String tag = "A" + tagSequence++;
            writer.write(tag + " " + command + "\r\n");
            writer.flush();
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
                if (line.startsWith(tag + " ")) {
                    if (!line.contains(" OK")) {
                        throw new IllegalStateException(commandName(command) + " 실패: " + line);
                    }
                    return response.toString();
                }
            }
            throw new IllegalStateException(commandName(command) + " 응답이 없습니다.");
        }

        private String commandName(String command) {
            int spaceIndex = command.indexOf(' ');
            return spaceIndex < 0 ? command : command.substring(0, spaceIndex);
        }
    }
}
