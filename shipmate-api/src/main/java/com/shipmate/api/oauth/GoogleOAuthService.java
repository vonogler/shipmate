package com.shipmate.api.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.config.GoogleOAuthProperties;
import com.shipmate.api.operation.audit.AuditLogService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class GoogleOAuthService {
    private static final String AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SCOPE = "openid email profile https://www.googleapis.com/auth/gmail.modify";

    private final GoogleOAuthProperties properties;
    private final GoogleOAuthRepository repository;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            GoogleOAuthRepository repository,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.repository = repository;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    void encryptStoredTokensIfNeeded() {
        for (GoogleOAuthToken token : repository.findAllTokens()) {
            if (repository.tokenNeedsEncryption(token)) {
                repository.saveToken(token);
            }
        }
    }

    public GoogleOAuthAuthorizeUrlResponse createAuthorizeUrl(GoogleOAuthAuthorizeUrlRequest request) {
        requireConfigured();
        String merchantId = requestAuthContext.currentMerchantIdOr(request == null ? null : request.merchantId());
        if (merchantId == null || merchantId.isBlank()) {
            throw new ApiRequestException("SM-OAU-9001", HttpStatus.BAD_REQUEST, "OAuth를 연결할 이용자 정보가 없습니다.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String state = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = now.plusMinutes(10);
        repository.saveState(new GoogleOAuthState(
                state,
                merchantId,
                requestAuthContext.currentActorIdOr(null),
                expiresAt,
                null,
                now
        ));

        String authorizationUrl = AUTHORIZE_ENDPOINT
                + "?client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(properties.redirectUri())
                + "&response_type=code"
                + "&scope=" + encode(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
        return new GoogleOAuthAuthorizeUrlResponse(authorizationUrl, state, expiresAt);
    }

    public String handleCallback(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            return html("Google OAuth 연결 실패", "Google 인증이 취소되었거나 실패했습니다: " + escape(error));
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return html("Google OAuth 연결 실패", "인증 응답에 필요한 code 또는 state가 없습니다.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        GoogleOAuthState savedState = repository.findState(state)
                .orElseThrow(() -> new ApiRequestException("SM-OAU-9002", HttpStatus.BAD_REQUEST, "유효하지 않은 OAuth state입니다."));
        if (!savedState.isUsable(now)) {
            throw new ApiRequestException("SM-OAU-9003", HttpStatus.BAD_REQUEST, "만료되었거나 이미 사용된 OAuth state입니다.");
        }

        JsonNode token = exchangeToken(code);
        String accessToken = token.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw new ApiRequestException("SM-OAU-9004", HttpStatus.BAD_GATEWAY, "Google access token을 받지 못했습니다.");
        }
        String refreshToken = token.path("refresh_token").isMissingNode() ? null : token.path("refresh_token").asText(null);
        String scope = token.path("scope").asText(null);
        String tokenType = token.path("token_type").asText(null);
        int expiresIn = token.path("expires_in").asInt(0);
        String googleEmail = fetchGoogleEmail(accessToken);

        repository.saveToken(new GoogleOAuthToken(
                savedState.merchantId(),
                googleEmail,
                accessToken,
                refreshToken,
                scope,
                tokenType,
                expiresIn > 0 ? now.plusSeconds(expiresIn) : null,
                now
        ));
        repository.markStateUsed(state, now);
        auditLogService.append(
                savedState.merchantId(),
                null,
                "GOOGLE_OAUTH_CONNECTED",
                "GOOGLE_OAUTH",
                googleEmail,
                "Google OAuth Gmail 읽기와 읽음 처리 권한이 연결되었습니다."
        );

        return html("Google OAuth 연결 완료", "Gmail 읽기와 읽음 처리 권한 연결이 완료되었습니다. 이 창을 닫고 출고비서로 돌아가도 됩니다.");
    }

    public GoogleOAuthStatusResponse status(String merchantId) {
        String readableMerchantId = requestAuthContext.currentMerchantIdOr(merchantId);
        if (readableMerchantId == null || readableMerchantId.isBlank()) {
            throw new ApiRequestException("SM-OAU-9008", HttpStatus.BAD_REQUEST, "OAuth 연결 상태를 확인할 이용자 정보가 없습니다.");
        }
        return repository.findTokenByMerchantId(readableMerchantId)
                .map(GoogleOAuthStatusResponse::connected)
                .orElseGet(() -> GoogleOAuthStatusResponse.disconnected(readableMerchantId));
    }

    private JsonNode exchangeToken(String code) {
        String body = "client_id=" + encode(properties.clientId())
                + "&client_secret=" + encode(properties.clientSecret())
                + "&code=" + encode(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(properties.redirectUri());
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendJson(request, "Google OAuth token 교환에 실패했습니다.");
    }

    private String fetchGoogleEmail(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonNode userInfo = sendJson(request, "Google 사용자 정보를 조회하지 못했습니다.");
        String email = userInfo.path("email").asText("");
        return email.isBlank() ? null : email;
    }

    private JsonNode sendJson(HttpRequest request, String failureMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiRequestException("SM-OAU-9005", HttpStatus.BAD_GATEWAY, failureMessage + " 상태: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (ApiRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiRequestException("SM-OAU-9006", HttpStatus.BAD_GATEWAY, failureMessage);
        }
    }

    private void requireConfigured() {
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret()) || isBlank(properties.redirectUri())) {
            throw new ApiRequestException("SM-OAU-9007", HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth 설정이 없습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String html(String title, String message) {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <title>%s</title>
                </head>
                <body>
                  <h1>%s</h1>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(escape(title), escape(title), escape(message));
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
