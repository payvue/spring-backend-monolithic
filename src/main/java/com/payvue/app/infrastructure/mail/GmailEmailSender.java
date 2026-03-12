package com.payvue.app.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Gmail SMTP 기반 이메일 발송 구현체
 */
@Component
@RequiredArgsConstructor
public class GmailEmailSender implements EmailSender {
    
    private final JavaMailSender mailSender;
    private final EmailTemplateProperties mailProperties;

    @Override
    public void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}
