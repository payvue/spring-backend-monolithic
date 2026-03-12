package com.payvue.app.domain.alarm;

import com.payvue.app.infrastructure.sse.AlarmSseMessage;
import com.payvue.app.infrastructure.sse.SseEmitterRegistry;
import com.payvue.generic.alarm.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
public class SseAlarmChannel implements AlarmChannel {

    private final SseEmitterRegistry emitterRegistry;

    @Override
    public void send(Alarm alarm) throws SendFailureException {
        if (!(alarm instanceof InAppAlarm)) {
            return;
        }

        String userId = alarm.getRecipient().getValue();
        SseEmitter emitter = emitterRegistry.get(userId);
        
        if (emitter == null) {
            return;
        }

        try {

            AlarmSseMessage message = AlarmSseMessage.from(alarm, false);

            emitter.send(
                    SseEmitter.event()
                            .name(message.event().type())
                            .data(message)
            );
            
            alarm.markAsSent();
            
        } catch (Exception e) {
            emitterRegistry.remove(userId);
            throw new SendFailureException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean supports(String channelKey) {
        return ChannelCategory.IN_APP.name().equals(channelKey);
    }
}
