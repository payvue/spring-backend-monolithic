package com.payvue.app.infrastructure.mail;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.mail.template")
public final class EmailTemplateProperties {
    private final String templatePrefix;
    private final String templateSuffix;
    private final String brandName;
    private final String footer;
    private final String defaultSubject;
    private final String from;
    private final String fromName;
}
