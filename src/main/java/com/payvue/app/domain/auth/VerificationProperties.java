package com.payvue.app.domain.auth;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "app.verification")
class VerificationProperties {
    private String baseUrl;
    private String endpoint;
}
