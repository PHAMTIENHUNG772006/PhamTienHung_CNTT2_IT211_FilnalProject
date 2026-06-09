package com.re.controller;

import com.re.model.dto.response.ApiDataResponse;
import com.re.model.dto.auth.AuthResponse;
import com.re.model.dto.auth.UserResponse;
import com.re.model.dto.auth.LoginRequest;
import com.re.model.dto.auth.RefreshTokenRequest;
import com.re.model.dto.auth.RegisterRequest;
import com.re.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiDataResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse userResponse =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiDataResponse<>(
                                true,
                                "Đăng ký thành công",
                                userResponse,
                                null,
                                HttpStatus.CREATED
                        )
                );
    }


    @PostMapping("/login")
    public ResponseEntity<ApiDataResponse<AuthResponse>> login(
          @Valid @RequestBody LoginRequest loginRequest
    ) {

        AuthResponse authResponse = authService.login(loginRequest);


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new ApiDataResponse<>(
                                true,
                                "Đăng nhập tài khoản thành công",
                                authResponse,
                                null,
                                HttpStatus.OK
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiDataResponse<AuthResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        new ApiDataResponse<>(
                                true,
                                "Cấp lại Token thành công",
                                authService.refreshToken(request),
                                null,
                                HttpStatus.OK
                        )
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra trạng thái xác thực của người dùng
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Bạn chưa đăng nhập hoặc phiên làm việc không hợp lệ");
        }


        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);


        String email = authentication.getName();


        authService.logout(accessToken, email);

        return ResponseEntity.ok("Đăng xuất thành công, toàn bộ Tokens đã bị hủy kích hoạt.");
    }
}