package com.payvue.app.domain.alarm;

import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.Recipient;

public class InAppAlarm extends Alarm {
    public InAppAlarm(
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
        return ChannelCategory.IN_APP.name();
    }
}
