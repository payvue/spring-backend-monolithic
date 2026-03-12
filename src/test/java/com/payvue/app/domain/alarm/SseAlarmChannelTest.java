package com.payvue.app.domain.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.payvue.app.infrastructure.sse.SseEmitterRegistry;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.Recipient;
import com.payvue.generic.alarm.SendFailureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseAlarmChannelTest {

    @Test
    @DisplayName("SSE 채널은 인앱 알림이 아니면 전송하지 않는다")
    void sendSkipsNonInAppAlarm() throws Exception {
        SseEmitterRegistry registry = mock(SseEmitterRegistry.class);
        SseAlarmChannel channel = new SseAlarmChannel(registry);

        Alarm alarm = new EmailAlarm(
                AlarmId.generate(),
                Recipient.ofUser("user"),
                "t",
                "m",
                "d",
                () -> "payment_request"
        );

        channel.send(alarm);

        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("SSE 채널은 emitter가 없으면 전송하지 않는다")
    void sendSkipsWhenEmitterMissing() throws Exception {
        SseEmitterRegistry registry = mock(SseEmitterRegistry.class);
        when(registry.get("user"))
                .thenReturn(null);

        SseAlarmChannel channel = new SseAlarmChannel(registry);
        TestInAppAlarm alarm = new TestInAppAlarm();

        channel.send(alarm);

        assertThat(alarm.sent).isFalse();
    }

    @Test
    @DisplayName("SSE 채널은 emitter가 있으면 전송 후 sent 상태로 변경한다")
    void sendMarksAsSent() throws Exception {
        SseEmitterRegistry registry = mock(SseEmitterRegistry.class);
        SseEmitter emitter = mock(SseEmitter.class);
        when(registry.get("user"))
                .thenReturn(emitter);

        SseAlarmChannel channel = new SseAlarmChannel(registry);
        TestInAppAlarm alarm = new TestInAppAlarm();

        channel.send(alarm);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(alarm.sent).isTrue();
    }

    @Test
    @DisplayName("SSE 채널은 전송 실패 시 emitter를 제거하고 예외를 던진다")
    void sendRemovesEmitterOnFailure() throws Exception {
        SseEmitterRegistry registry = mock(SseEmitterRegistry.class);
        SseEmitter emitter = mock(SseEmitter.class);
        when(registry.get("user"))
                .thenReturn(emitter);
        doThrow(new RuntimeException("boom"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));

        SseAlarmChannel channel = new SseAlarmChannel(registry);
        TestInAppAlarm alarm = new TestInAppAlarm();

        assertThatThrownBy(() -> channel.send(alarm))
                .isInstanceOf(SendFailureException.class);
        verify(registry).remove("user");
    }

    private static class TestInAppAlarm extends InAppAlarm {
        private boolean sent;

        TestInAppAlarm() {
            super(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", new TestContent());
        }

        @Override
        public void markAsSent() {
            this.sent = true;
        }
    }

    private static class TestContent implements AlarmContent {
        @Override
        public String operationKey() {
            return "payment_request";
        }
    }
}
