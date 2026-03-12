package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.alarm.InAppAlarm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultChannelRouterTest {

    @Test
    @DisplayName("라우터는 알림 채널을 찾아 전송한다")
    void routeFindsMatchingChannel() throws Exception {
        DefaultChannelRouter router = new DefaultChannelRouter();
        CapturingChannel channel = new CapturingChannel("IN_APP");
        router.register(channel);

        Alarm alarm = new InAppAlarm(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", () -> "op");
        router.route(alarm, AlarmContext.builder().router(router).repository(new InMemoryRepository()).build());

        assertThat(channel.sentAlarm).isSameAs(alarm);
    }

    @Test
    @DisplayName("라우터는 채널이 없으면 예외를 던진다")
    void routeThrowsWhenNoChannel() {
        DefaultChannelRouter router = new DefaultChannelRouter();
        Alarm alarm = new InAppAlarm(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", () -> "op");

        assertThatThrownBy(() -> router.route(
                alarm,
                AlarmContext.builder().router(router).repository(new InMemoryRepository()).build()
        ))
                .isInstanceOf(SendFailureException.class)
                .hasMessageContaining("no channel for category");
    }

    private static class CapturingChannel implements AlarmChannel {
        private final String channelKey;
        private Alarm sentAlarm;

        private CapturingChannel(String channelKey) {
            this.channelKey = channelKey;
        }

        @Override
        public void send(Alarm alarm) {
            this.sentAlarm = alarm;
        }

        @Override
        public boolean supports(String candidate) {
            return channelKey.equals(candidate);
        }
    }

    private static class InMemoryRepository implements AlarmRepository {
        @Override
        public Alarm save(Alarm alarm) {
            return alarm;
        }

        @Override
        public java.util.Optional<Alarm> findById(AlarmId alarmId) {
            return java.util.Optional.empty();
        }
    }
}
