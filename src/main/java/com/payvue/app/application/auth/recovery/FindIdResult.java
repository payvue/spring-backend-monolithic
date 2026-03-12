package com.payvue.app.application.auth.recovery;

public record FindIdResult(
        String loginId,
        String maskedEmail
) {}
