package com.shipmate.api.auth;

import com.shipmate.api.common.ApiRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {
    public static final String INTERNAL_MERCHANT_ID = "shipmate_internal";

    private final MerchantUserRepository repository;

    public AuthService(MerchantUserRepository repository) {
        this.repository = repository;
    }

    public LoginResponse register(RegisterMerchantUserRequest request) {
        repository.findByLoginId(request.loginId()).ifPresent(user -> {
            throw new ApiRequestException("SM-AUT-8001", HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다.");
        });
        OffsetDateTime now = OffsetDateTime.now();
        MerchantUser user = new MerchantUser(
                "musr_" + UUID.randomUUID().toString().replace("-", ""),
                request.merchantId().strip(),
                request.loginId().strip(),
                hash(request.password()),
                request.displayName().strip(),
                request.role().strip(),
                null,
                now,
                now
        );
        repository.save(user);
        return LoginResponse.from(user, token(user));
    }

    public LoginResponse registerInternal(String loginId, String password, String displayName, String role) {
        String normalizedRole = normalizeInternalRole(role);
        return register(new RegisterMerchantUserRequest(
                INTERNAL_MERCHANT_ID,
                loginId,
                password,
                displayName,
                normalizedRole
        ));
    }

    public LoginResponse login(LoginRequest request) {
        MerchantUser user = repository.findByLoginId(request.loginId())
                .orElseThrow(() -> new ApiRequestException("SM-AUT-8002", HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다."));
        if (!user.passwordHash().equals(hash(request.password()))) {
            throw new ApiRequestException("SM-AUT-8002", HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.");
        }
        if (user.lockedAt() != null) {
            throw new ApiRequestException("SM-AUT-8008", HttpStatus.FORBIDDEN, "잠금 처리된 계정입니다.");
        }
        return LoginResponse.from(user, token(user));
    }

    public LoginResponse loginInternal(LoginRequest request) {
        LoginResponse response = login(request);
        if (!INTERNAL_MERCHANT_ID.equals(response.merchantId())) {
            throw new ApiRequestException("SM-AUT-8006", HttpStatus.FORBIDDEN, "내부관리자 계정만 로그인할 수 있습니다.");
        }
        return response;
    }

    public MerchantUser authenticateToken(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", -1);
            if (parts.length != 4) {
                throw unauthorizedToken();
            }
            MerchantUser user = repository.findById(parts[0])
                    .orElseThrow(this::unauthorizedToken);
            if (user.lockedAt() != null) {
                throw unauthorizedToken();
            }
            String tokenBody = "%s:%s:%s".formatted(parts[0], parts[1], parts[2]);
            if (!user.merchantId().equals(parts[1]) || !signature(tokenBody, user.passwordHash()).equals(parts[3])) {
                throw unauthorizedToken();
            }
            return user;
        } catch (ApiRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorizedToken();
        }
    }

    private String token(MerchantUser user) {
        String tokenBody = "%s:%s:%s".formatted(user.merchantUserId(), user.merchantId(), System.currentTimeMillis());
        String signedTokenBody = tokenBody + ":" + signature(tokenBody, user.passwordHash());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedTokenBody.getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String tokenBody, String passwordHash) {
        return hash(tokenBody + ":" + passwordHash);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("비밀번호 해시를 생성하지 못했습니다.", exception);
        }
    }

    public String passwordHash(String password) {
        return hash(password);
    }

    private String normalizeInternalRole(String role) {
        String normalizedRole = role == null || role.isBlank() ? "VIEWER" : role.strip();
        if (normalizedRole.equals("ADMIN") || normalizedRole.equals("OPERATOR") || normalizedRole.equals("VIEWER")) {
            return normalizedRole;
        }
        throw new ApiRequestException("SM-AUT-8007", HttpStatus.BAD_REQUEST, "내부관리자 권한 값이 올바르지 않습니다.");
    }

    private ApiRequestException unauthorizedToken() {
        return new ApiRequestException("SM-AUT-8004", HttpStatus.UNAUTHORIZED, "로그인 토큰이 올바르지 않습니다.");
    }
}
