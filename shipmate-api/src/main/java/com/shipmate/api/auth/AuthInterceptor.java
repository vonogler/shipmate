package com.shipmate.api.auth;

import com.shipmate.api.common.ApiRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final RequestAuthContext requestAuthContext;

    public AuthInterceptor(AuthService authService, RequestAuthContext requestAuthContext) {
        this.authService = authService;
        this.requestAuthContext = requestAuthContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicRequest(request)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiRequestException("SM-AUT-8003", HttpStatus.UNAUTHORIZED, "로그인이 필요한 요청입니다.");
        }

        requestAuthContext.set(authService.authenticateToken(authorization.substring("Bearer ".length())));
        requireRoleIfNeeded(request);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        requestAuthContext.clear();
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/auth/")
                || path.equals("/api/admin/auth/login")
                || path.equals("/api/admin/auth/register")
                || path.equals("/api/oauth/google/callback")
                || path.equals("/api/health")) {
            return true;
        }
        return path.equals("/api/inbound-messages") && method.equalsIgnoreCase("POST");
    }

    private void requireRoleIfNeeded(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/admin")) {
            requireInternalAdminAccount();
            if (path.equals("/api/admin/users") || path.equals("/api/admin/backup")) {
                requireAnyRole("ADMIN");
                return;
            }
            requireAnyRole("ADMIN", "OPERATOR", "VIEWER");
            if (!method.equalsIgnoreCase("GET")) {
                requireAnyRole("ADMIN");
            }
            return;
        }
        if (path.equals("/api/operation/backups/restore-approve")
                || (path.startsWith("/api/operation/settings") && method.equalsIgnoreCase("POST"))
                || path.startsWith("/api/email-orders/mailbox-config")
                || method.equalsIgnoreCase("DELETE")
                || (path.startsWith("/api/customers") && method.equalsIgnoreCase("POST"))) {
            requireAnyRole("ADMIN");
            return;
        }
        if ((path.contains("/submit") || path.contains("/review") || path.startsWith("/api/shipping-orders/courier-csv"))
                && !method.equalsIgnoreCase("GET")) {
            requireAnyRole("ADMIN", "OPERATOR");
        }
    }

    private void requireAnyRole(String... roles) {
        String currentRole = requestAuthContext.currentUser()
                .map(MerchantUser::role)
                .orElse("");
        for (String role : roles) {
            if (currentRole.equals(role)) {
                return;
            }
        }
        throw new ApiRequestException("SM-AUT-8005", HttpStatus.FORBIDDEN, "요청 권한이 없습니다.");
    }

    private void requireInternalAdminAccount() {
        boolean internalAccount = requestAuthContext.currentUser()
                .map(user -> AuthService.INTERNAL_MERCHANT_ID.equals(user.merchantId()))
                .orElse(false);
        if (!internalAccount) {
            throw new ApiRequestException("SM-AUT-8006", HttpStatus.FORBIDDEN, "내부관리자 계정만 접근할 수 있습니다.");
        }
    }
}
