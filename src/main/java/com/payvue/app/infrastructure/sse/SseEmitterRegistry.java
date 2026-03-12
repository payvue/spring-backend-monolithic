package com.payvue.app.infrastructure.sse;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@NoArgsConstructor
public class SseEmitterRegistry {

    private static final long DEFAULT_TIMEOUT = 60L * 60 * 1000;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT) {
            @Override
            public void complete() {
                super.complete();
                emitters.remove(userId);
            }

            @Override
            public void completeWithError(Throwable ex) {
                super.completeWithError(ex);
                emitters.remove(userId);
            }
        };
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(error -> emitters.remove(userId));

        return emitter;
    }

    public SseEmitter get(String userId) {
        return emitters.get(userId);
    }

    public void remove(String userId) {
        emitters.remove(userId);
    }
}
