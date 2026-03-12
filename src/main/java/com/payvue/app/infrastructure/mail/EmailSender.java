package com.payvue.app.infrastructure.mail;

/**
 * 이메일 발송 인터페이스
 */
public interface EmailSender {
    void send(String to, String subject, String htmlContent);
}
