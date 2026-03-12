package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FailureHandlerTest {

    @Test
    @DisplayName("FailureHandler는 성공할 때까지 재시도한다")
    void retriesUntilSuccess() {
        CountingRouter router = new CountingRouter(2);
        AlarmContext context = AlarmContext.builder()
                .router(router)
                .repository(new InMemoryRepository())
                .build();
        FailureHandler handler = new FailureHandler();
        TestAlarm alarm = new TestAlarm();

        handler.handle(alarm, new SendFailureException("fail"), context);

        assertThat(router.attempts).isEqualTo(2);
        assertThat(alarm.failed).isFalse();
    }

    @Test
    @DisplayName("FailureHandler는 재시도 실패 시 알림을 실패 처리한다")
    void marksFailedAfterRetries() {
        CountingRouter router = new CountingRouter(5);
        AlarmContext context = AlarmContext.builder()
                .router(router)
                .repository(new InMemoryRepository())
                .build();
        FailureHandler handler = new FailureHandler();
        TestAlarm alarm = new TestAlarm();

        handler.handle(alarm, new SendFailureException("fail"), context);

        assertThat(router.attempts).isEqualTo(3);
        assertThat(alarm.failed).isTrue();
    }

    private static class TestAlarm extends Alarm {
        private boolean failed;

        TestAlarm() {
            super(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", () -> "op");
        }

        @Override
        public void markAsFailed(String reason) {
            this.failed = true;
        }

        @Override
        protected String channelKey() {
            return "IN_APP";
        }
    }

    private static class CountingRouter implements ChannelRouter {
        private final int successOnAttempt;
        private int attempts;

        private CountingRouter(int successOnAttempt) {
            this.successOnAttempt = successOnAttempt;
        }

        @Override
        public void register(AlarmChannel channel) {
        }

        @Override
        public void route(Alarm alarm, AlarmContext context) throws SendFailureException {
            attempts++;
            if (attempts < successOnAttempt) {
                throw new SendFailureException("fail");
            }
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
