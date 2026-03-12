package com.payvue.app.domain.alarm;

import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.Recipient;
import lombok.Getter;

@Getter
public final class EmailAlarm extends Alarm {

    public EmailAlarm(
            AlarmId id,
            Recipient recipient,
            String title,
            String message,
            String description,
            AlarmContent content
    ) {
        super(id, recipient, title, message, description, content);
    }

    @Override
    public String channelKey() {
        return ChannelCategory.EMAIL.name();
    }
}
