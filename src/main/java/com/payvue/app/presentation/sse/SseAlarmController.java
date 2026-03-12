package com.payvue.app.presentation.sse;

import com.payvue.app.infrastructure.sse.SseEmitterRegistry;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sse")
public class SseAlarmController {

    private final SseEmitterRegistry emitterRegistry;
    private final UserContextManager userContextManager;

    public SseAlarmController(SseEmitterRegistry emitterRegistry, UserContextManager userContextManager) {
        this.emitterRegistry = emitterRegistry;
        this.userContextManager = userContextManager;
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails user) {
        User domainUser = userContextManager.findByEmail(user.getUsername(), User.class)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String userId = domainUser.getId().toString();

        SseEmitter emitter = emitterRegistry.register(userId);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            emitterRegistry.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
