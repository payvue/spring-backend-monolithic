package com.payvue.app.domain.auth;

public record AuthSession(String accessToken, String refreshToken) {}
