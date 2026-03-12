package com.payvue.app.application.auth;

import com.payvue.app.application.auth.recovery.FindIdResult;
import com.payvue.app.application.auth.recovery.ResetPasswordVerifyResult;

public interface VerificationUseCase {

    void verifySignup(String token);

    FindIdResult verifyFindId(String token);

    ResetPasswordVerifyResult verifyResetPassword(String token);

    void resetPasswordConfirm(String token, String newPassword);

    void resendSignupVerification(String email);
}
