package com.payvue.app.domain.alarm;

import com.payvue.app.infrastructure.mail.EmailTemplateId;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmOperationUnit;

import java.util.Map;

public record verificationEmailContent(String username, String action, String verifyUrl, EmailTemplateId emailTemplateId) implements AlarmContent {
    @Override
    public String operationKey() {
        return AlarmOperationUnit.VERIFICATION.key();
    }

    public Map<String, Object> toVariables() {
        return Map.of(
                "userName", username,
                "action", action,
                "verifyUrl", verifyUrl
        );
    }
}
