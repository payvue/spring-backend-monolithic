package com.payvue.app.application.auth.recovery;

public record ResetPasswordVerifyResult(
        String temporaryPassword
) {}
