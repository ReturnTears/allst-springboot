package com.allst.springboot.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenView {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresInSeconds;
    private final boolean passwordExpired;
}
