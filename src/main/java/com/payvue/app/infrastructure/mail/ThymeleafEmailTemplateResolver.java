package com.payvue.app.infrastructure.mail;

import lombok.AllArgsConstructor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@AllArgsConstructor
public final class ThymeleafEmailTemplateResolver implements EmailTemplateResolver {

    private final TemplateEngine templateEngine;
    private final EmailTemplateProperties properties;


    @Override
    public EmailTemplate resolve(EmailTemplateId templateId, Map<String, Object> variables) {
        Map<String, Object> payload = new HashMap<>();
        if (variables != null) {
            payload.putAll(variables);
        }

        String subject = (String) payload.getOrDefault("subject", properties.getDefaultSubject());
        payload.put("subject", subject);
        payload.putIfAbsent("brandName", properties.getBrandName());
        payload.putIfAbsent("footer", properties.getFooter());

        Context context = new Context(Locale.KOREAN);
        context.setVariables(payload);

        String templatePath = properties.getTemplatePrefix()
                + templateId.name().toLowerCase(Locale.ROOT)
                + properties.getTemplateSuffix();

        String body = templateEngine.process(templatePath, context);
        return new EmailTemplate(subject, body);
    }
}
