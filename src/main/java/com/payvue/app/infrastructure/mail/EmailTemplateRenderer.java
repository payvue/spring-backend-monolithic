package com.payvue.app.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Thymeleaf 기반 이메일 템플릿 렌더러
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {
    
    private final TemplateEngine templateEngine;

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        
        return templateEngine.process(templateName, context);
    }
}
