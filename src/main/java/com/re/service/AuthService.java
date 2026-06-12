package com.re.service;

import com.re.model.dto.auth.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {


    UserResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(HttpServletRequest request);

    String forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);
}