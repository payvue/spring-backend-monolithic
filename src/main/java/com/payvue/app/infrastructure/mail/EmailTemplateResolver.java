package com.payvue.app.infrastructure.mail;

import java.util.Map;

public interface EmailTemplateResolver {
    EmailTemplate resolve(EmailTemplateId templateId, Map<String, Object> variables);
}
