package com.re.service;

import com.re.model.dto.auth.AuthResponse;
import com.re.model.dto.auth.UserResponse;
import com.re.model.dto.auth.LoginRequest;
import com.re.model.dto.auth.RefreshTokenRequest;
import com.re.model.dto.auth.RegisterRequest;

public interface AuthService {


    UserResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);


    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken, String email);
}