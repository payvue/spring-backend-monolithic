package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.alarm.EmailAlarm;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.app.domain.alarm.SystemAlarmContent;
import com.payvue.app.domain.alarm.verificationEmailContent;
import com.payvue.app.infrastructure.mail.EmailSender;
import com.payvue.app.infrastructure.mail.EmailTemplate;
import com.payvue.app.infrastructure.mail.EmailTemplateId;
import com.payvue.app.infrastructure.mail.EmailTemplateResolver;
import com.payvue.app.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest(classes = {
        AlarmContextConfiguration.class
})
class AlarmContextConfigurationBootTest {

    @Autowired
    private AlarmContextManager alarmContextManager;

    @MockBean
    private AlarmRepository alarmRepository;

    @MockBean
    private EmailSender emailSender;

    @MockBean
    private EmailTemplateResolver emailTemplateResolver;

    @MockBean
    private SseEmitterRegistry sseEmitterRegistry;

    @Test
    @DisplayName("AlarmContextConfiguration은 AlarmContextManager 빈을 제공한다")
    void providesAlarmContextManager() {
        assertThat(alarmContextManager).isNotNull();
    }

    @Test
    @DisplayName("AlarmContextManager는 EMAIL 채널 알림을 전송한다")
    void managerSendsEmailAlarm() {
        when(emailTemplateResolver.resolve(eq(EmailTemplateId.VERIFICATION), any()))
                .thenReturn(new EmailTemplate("subject", "body"));

        Alarm alarm = new EmailAlarm(
                AlarmId.generate(),
                Recipient.ofUser("user@payvue.com"),
                "t",
                "m",
                "d",
                new verificationEmailContent(
                        "Alice",
                        "signup",
                        "https://example.com",
                        EmailTemplateId.VERIFICATION
                )
        );

        alarmContextManager.send(alarm);

        verify(emailTemplateResolver).resolve(eq(EmailTemplateId.VERIFICATION), any());
        verify(emailSender).send(eq("user@payvue.com"), eq("subject"), eq("body"));
        verify(alarmRepository, times(2)).save(alarm);
    }

    @Test
    @DisplayName("AlarmContextManager는 IN_APP 채널 알림을 전송한다")
    void managerSendsInAppAlarm() throws Exception {
        SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);
        when(sseEmitterRegistry.get("user")).thenReturn(emitter);

        TestInAppAlarm alarm = new TestInAppAlarm();

        alarmContextManager.send(alarm);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(alarmRepository, times(2)).save(alarm);
        assertThat(alarm.sent).isTrue();
    }

    @Test
    @DisplayName("AlarmContextManager는 전송 실패 시 예외 핸들러로 재시도 후 실패 처리한다")
    void managerRetriesAndMarksFailedOnSendFailure() throws Exception {
        SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);
        when(sseEmitterRegistry.get("user")).thenReturn(emitter);
        doThrow(new RuntimeException("boom")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        TestInAppAlarm alarm = new TestInAppAlarm();

        alarmContextManager.send(alarm);

        verify(emitter, times(4)).send(any(SseEmitter.SseEventBuilder.class));
        verify(alarmRepository, times(1)).save(alarm);
        assertThat(alarm.failed).isTrue();
    }

    @Test
    @DisplayName("AlarmContextManager는 등록되지 않은 채널 키면 실패 처리한다")
    void managerMarksFailedWhenChannelMissing() {
        UnknownChannelAlarm alarm = new UnknownChannelAlarm();

        alarmContextManager.send(alarm);

        verifyNoInteractions(sseEmitterRegistry);
        verify(alarmRepository, times(1)).save(alarm);
        assertThat(alarm.failed).isTrue();
    }

    private static class TestInAppAlarm extends InAppAlarm {
    private boolean sent;
    private boolean failed;

    TestInAppAlarm() {
            this(new SystemAlarmContent(
                    AlarmOperationUnit.SECURITY,
                    "device",
                    "location",
                    "127.0.0.1",
                    "reason"
            ));
        }

        TestInAppAlarm(AlarmContent content) {
            super(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", content);
        }

        @Override
        public void markAsSent() {
            this.sent = true;
        }

        @Override
        public void markAsFailed(String reason) {
            this.failed = true;
        }
    }

    private static class UnknownChannelAlarm extends Alarm {
        private boolean failed;

        UnknownChannelAlarm() {
            super(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", () -> "op");
        }

        @Override
        protected String channelKey() {
            return "UNKNOWN";
        }

        @Override
        public void markAsFailed(String reason) {
            this.failed = true;
        }
    }
}
