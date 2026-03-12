package com.payvue.app.infrastructure.mail;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


@Configuration
@EnableConfigurationProperties(EmailTemplateProperties.class)
public class ThymeleafEmailTemplateResolverConfiguration {

    @Bean
    public TemplateEngine emailTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(true);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    @Bean
    public EmailTemplateResolver emailTemplateResolver(
            TemplateEngine emailTemplateEngine,
            EmailTemplateProperties emailTemplateProperties
    ) {
        return new ThymeleafEmailTemplateResolver(
                emailTemplateEngine,
                emailTemplateProperties
        );
    }
}
