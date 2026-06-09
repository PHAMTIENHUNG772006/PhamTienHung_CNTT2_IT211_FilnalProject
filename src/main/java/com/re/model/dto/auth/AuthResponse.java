package com.re.model.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;
}