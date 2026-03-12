package com.payvue.generic.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultAlarmContextManagerTest {

    @Test
    @DisplayName("알림 전송은 저장 후 라우팅하고 다시 저장한다")
    void sendPersistsBeforeAndAfterRouting() {
        RecordingRepository repository = new RecordingRepository();
        CapturingRouter router = new CapturingRouter();
        AlarmContext context = AlarmContext.builder()
                .router(router)
                .repository(repository)
                .build();

        DefaultAlarmContextManager manager = new DefaultAlarmContextManager(context);
        Alarm alarm = new TestAlarm();

        manager.send(alarm);

        assertThat(repository.saved).containsExactly(alarm, alarm);
        assertThat(router.routed).containsExactly(alarm);
    }

    @Test
    @DisplayName("라우팅 실패 시 예외 핸들러를 호출한다")
    void sendUsesExceptionHandlerOnFailure() {
        RecordingRepository repository = new RecordingRepository();
        FailingRouter router = new FailingRouter();
        RecordingHandler handler = new RecordingHandler();
        AlarmContext context = AlarmContext.builder()
                .router(router)
                .repository(repository)
                .exceptionHandler(SendFailureException.class, handler)
                .build();

        DefaultAlarmContextManager manager = new DefaultAlarmContextManager(context);
        Alarm alarm = new TestAlarm();

        manager.send(alarm);

        assertThat(repository.saved).containsExactly(alarm);
        assertThat(handler.handledAlarm).isSameAs(alarm);
    }

    private static class TestAlarm extends Alarm {
        TestAlarm() {
            super(AlarmId.generate(), Recipient.ofUser("user"), "t", "m", "d", () -> "op");
        }

        @Override
        protected String channelKey() {
            return "IN_APP";
        }
    }

    private static class RecordingRepository implements AlarmRepository {
        private final List<Alarm> saved = new ArrayList<>();

        @Override
        public Alarm save(Alarm alarm) {
            saved.add(alarm);
            return alarm;
        }

        @Override
        public java.util.Optional<Alarm> findById(AlarmId alarmId) {
            return java.util.Optional.empty();
        }
    }

    private static class CapturingRouter implements ChannelRouter {
        private final List<Alarm> routed = new ArrayList<>();

        @Override
        public void register(AlarmChannel channel) {
        }

        @Override
        public void route(Alarm alarm, AlarmContext context) {
            routed.add(alarm);
        }
    }

    private static class FailingRouter implements ChannelRouter {
        @Override
        public void register(AlarmChannel channel) {
        }

        @Override
        public void route(Alarm alarm, AlarmContext context) throws SendFailureException {
            throw new SendFailureException("fail");
        }
    }

    private static class RecordingHandler implements AlarmExceptionHandler<SendFailureException> {
        private Alarm handledAlarm;

        @Override
        public void handle(Alarm alarm, SendFailureException exception, AlarmContext context) {
            handledAlarm = alarm;
        }
    }
}
