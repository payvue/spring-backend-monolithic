package com.payvue.app.domain.alarm;

import com.payvue.app.infrastructure.mail.EmailSender;
import com.payvue.app.infrastructure.mail.EmailTemplate;
import com.payvue.app.infrastructure.mail.EmailTemplateResolver;
import com.payvue.generic.alarm.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;


@RequiredArgsConstructor
public class EmailAlarmChannel implements AlarmChannel {

    private final EmailSender emailSender;
    private final EmailTemplateResolver emailTemplateResolver;

    @Override
    @Async("emailTaskExecutor")
    public void send(Alarm alarm) throws SendFailureException {

        try {
            if(alarm instanceof EmailAlarm emailAlarm) {

                if(emailAlarm.getContent() instanceof verificationEmailContent content) {
                    EmailTemplate template = emailTemplateResolver.resolve(
                            content.emailTemplateId(),
                            content.toVariables()
                    );

                    emailSender.send(
                            alarm.getRecipient().getValue(),
                            template.subject(),
                            template.body()
                    );
                }
            }
        } catch (Exception e) {
            throw new SendFailureException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean supports(String channelKey) {
        return ChannelCategory.EMAIL.name().equals(channelKey);
    }

}
