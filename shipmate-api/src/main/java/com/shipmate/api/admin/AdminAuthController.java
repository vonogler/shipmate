package com.shipmate.api.admin;

import com.shipmate.api.auth.AuthService;
import com.shipmate.api.auth.LoginRequest;
import com.shipmate.api.auth.LoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.loginInternal(request);
    }

    @PostMapping("/register")
    LoginResponse register(@Valid @RequestBody AdminRegisterRequest request) {
        return authService.registerInternal(
                request.loginId(),
                request.password(),
                request.displayName(),
                request.role()
        );
    }

    public record AdminRegisterRequest(
            @NotBlank @Size(max = 100) String loginId,
            @NotBlank @Size(min = 4, max = 100) String password,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(max = 50) String role
    ) {
    }
}
