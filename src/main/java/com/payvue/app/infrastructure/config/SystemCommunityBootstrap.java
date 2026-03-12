package com.payvue.app.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemCommunityBootstrap implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
    }
}
