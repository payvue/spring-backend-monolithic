package com.payvue.app.application.auth;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.app.application.auth.verify.VerificationFindIdFlow;
import com.payvue.app.application.auth.verify.VerificationResetConfirmFlow;
import com.payvue.app.application.auth.verify.VerificationResetPasswordFlow;
import com.payvue.app.application.auth.verify.VerificationResendFlow;
import com.payvue.app.application.auth.verify.VerificationSignupFlow;
import com.payvue.app.application.auth.recovery.FindIdResult;
import com.payvue.app.application.auth.recovery.ResetPasswordVerifyResult;
import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificationApplicationService implements VerificationUseCase {

    private final AuthContextManager authContextManager;
    private final UserContextManager userContextManager;
    private final PasswordEncoder passwordEncoder;
    private final CommunityContextManager communityContextManager;
    private final AlarmContextManager alarmChannelRouter;
    private final OnlineCashManager cashManager;

    @Override
    @Transactional
    public void verifySignup(String token) {
        VerificationSignupFlow.start(token)
            .auth(step -> step.verify(authContextManager))
            .user(step -> step.loadUser(userContextManager))
            .verify(VerificationSignupFlow::ensureNotVerified)
            .community(step -> step.initPersonalCommunity(communityContextManager))
            .asset(step -> step.initializeAssets(cashManager, 0L))
            .user(step -> step.activateUser(userContextManager))
            .end();
    }

    @Override
    @Transactional(readOnly = true)
    public FindIdResult verifyFindId(String token) {
        return VerificationFindIdFlow.start(token)
            .auth(step -> step.verify(authContextManager))
            .user(step -> step.loadUser(userContextManager))
            .map(VerificationFindIdFlow::toResult)
            .end();
    }

    @Override
    @Transactional(readOnly = true)
    public ResetPasswordVerifyResult verifyResetPassword(String token) {
        return VerificationResetPasswordFlow.start(token)
            .auth(step -> step.verify(authContextManager))
            .user(step -> step.loadUserById(userContextManager))
            .user(step -> step.issueTemporaryPassword(passwordEncoder))
            .save(step -> step.persist(userContextManager))
            .map(VerificationResetPasswordFlow::toResult)
            .end();
    }

    @Override
    @Transactional
    public void resetPasswordConfirm(String token, String newPassword) {
        VerificationResetConfirmFlow.start(token, newPassword)
            .auth(step -> step.verify(authContextManager))
            .user(step -> step.loadUserById(userContextManager))
            .password(step -> step.changePassword(passwordEncoder))
            .save(step -> step.persist(userContextManager))
            .end();
    }

    @Override
    @Transactional
    public void resendSignupVerification(String email) {
        VerificationResendFlow.start(email)
            .user(step -> step.loadUser(userContextManager))
            .verify(VerificationResendFlow::ensureNotVerified)
            .auth(step -> step.issue(authContextManager))
            .notify(step -> step.sendSignupEmail(alarmChannelRouter))
            .end();
    }
}
