package com.payvue.app.domain.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.infrastructure.mail.EmailSender;
import com.payvue.app.infrastructure.mail.EmailTemplate;
import com.payvue.app.infrastructure.mail.EmailTemplateId;
import com.payvue.app.infrastructure.mail.EmailTemplateResolver;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.Recipient;
import com.payvue.generic.alarm.SendFailureException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailAlarmChannelTest {

    @Test
    @DisplayName("Email 채널은 인증 이메일을 템플릿으로 전송한다")
    void sendUsesTemplateAndSender() throws Exception {
        CapturingEmailSender sender = new CapturingEmailSender();
        CapturingTemplateResolver resolver = new CapturingTemplateResolver();
        EmailAlarmChannel channel = new EmailAlarmChannel(sender, resolver);

        Alarm alarm = new EmailAlarm(
                AlarmId.generate(),
                Recipient.ofUser("user@payvue.com"),
                "t",
                "m",
                "d",
                new verificationEmailContent("Alice", "signup", "https://example.com", EmailTemplateId.VERIFICATION)
        );

        channel.send(alarm);

        assertThat(resolver.requestedTemplateId).isEqualTo(EmailTemplateId.VERIFICATION);
        assertThat(resolver.requestedVariables)
                .containsEntry("userName", "Alice")
                .containsEntry("action", "signup")
                .containsEntry("verifyUrl", "https://example.com");
        assertThat(sender.to).isEqualTo("user@payvue.com");
        assertThat(sender.subject).isEqualTo("subject");
        assertThat(sender.body).isEqualTo("body");
    }

    @Test
    @DisplayName("Email 채널은 EmailAlarm이 아니면 전송하지 않는다")
    void sendSkipsNonEmailAlarm() throws Exception {
        CapturingEmailSender sender = new CapturingEmailSender();
        CapturingTemplateResolver resolver = new CapturingTemplateResolver();
        EmailAlarmChannel channel = new EmailAlarmChannel(sender, resolver);

        Alarm alarm = new InAppAlarm(
                AlarmId.generate(),
                Recipient.ofUser("user"),
                "t",
                "m",
                "d",
                () -> "op"
        );

        channel.send(alarm);

        assertThat(sender.to).isNull();
        assertThat(resolver.requestedTemplateId).isNull();
    }

    @Test
    @DisplayName("Email 채널은 전송 실패 시 SendFailureException을 던진다")
    void sendWrapsFailure() {
        EmailSender sender = (to, subject, body) -> {
            throw new IllegalStateException("boom");
        };
        EmailTemplateResolver resolver = (templateId, variables) -> new EmailTemplate("subject", "body");
        EmailAlarmChannel channel = new EmailAlarmChannel(sender, resolver);

        Alarm alarm = new EmailAlarm(
                AlarmId.generate(),
                Recipient.ofUser("user@payvue.com"),
                "t",
                "m",
                "d",
                new verificationEmailContent("Alice", "signup", "https://example.com", EmailTemplateId.VERIFICATION)
        );

        assertThatThrownBy(() -> channel.send(alarm))
                .isInstanceOf(SendFailureException.class);
    }

    private static class CapturingEmailSender implements EmailSender {
        private String to;
        private String subject;
        private String body;

        @Override
        public void send(String to, String subject, String htmlContent) {
            this.to = to;
            this.subject = subject;
            this.body = htmlContent;
        }
    }

    private static class CapturingTemplateResolver implements EmailTemplateResolver {
        private EmailTemplateId requestedTemplateId;
        private Map<String, Object> requestedVariables;

        @Override
        public EmailTemplate resolve(EmailTemplateId templateId, Map<String, Object> variables) {
            this.requestedTemplateId = templateId;
            this.requestedVariables = variables;
            return new EmailTemplate("subject", "body");
        }
    }
}
