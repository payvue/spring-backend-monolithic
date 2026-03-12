package com.payvue.generic.alarm;

import com.payvue.app.domain.alarm.ChannelCategory;
import com.payvue.app.domain.alarm.EmailAlarmChannel;
import com.payvue.app.domain.alarm.SseAlarmChannel;
import com.payvue.app.infrastructure.mail.EmailSender;
import com.payvue.app.infrastructure.mail.EmailTemplateResolver;
import com.payvue.app.infrastructure.mail.ThymeleafEmailTemplateResolverConfiguration;
import com.payvue.app.infrastructure.sse.SseEmitterRegistry;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ThymeleafEmailTemplateResolverConfiguration.class)
public class AlarmContextConfiguration {

    @Bean
    public AlarmContextManager alarmContextManager(
            EmailSender gmailEmailSender,
            EmailTemplateResolver emailTemplateResolver,
            SseEmitterRegistry sseEmitterRegistry,
            AlarmRepository alarmRepository,
            ObjectProvider<List<AlarmChannel>> customChannelsProvider
    ) {
        DefaultChannelRouter router = new DefaultChannelRouter();
        AlarmContext.Builder builder = AlarmContext.builder()
                .router(router)
                .repository(alarmRepository)
                .exceptionHandler(SendFailureException.class, new FailureHandler());

        builder
            .channel(ChannelCategory.EMAIL.name(), new EmailAlarmChannel(gmailEmailSender, emailTemplateResolver))
            .channel(ChannelCategory.IN_APP.name(), new SseAlarmChannel(sseEmitterRegistry));

        List<AlarmChannel> customChannels = customChannelsProvider.getIfAvailable(List::of);
        for (AlarmChannel channel : customChannels) {
            if (channel != null) {
                router.registerCustom(channel);
            }
        }

        AlarmContext context = builder.build();

        return new DefaultAlarmContextManager(context);
    }
}
