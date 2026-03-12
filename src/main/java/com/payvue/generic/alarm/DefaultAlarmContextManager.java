package com.payvue.generic.alarm;

import com.payvue.generic.alarm.AlarmContextManager;

import org.springframework.transaction.annotation.Transactional;

class DefaultAlarmContextManager implements AlarmContextManager {
    private final AlarmContext context;

    DefaultAlarmContextManager(AlarmContext context) {
        this.context = context;
    }

    @Override
    @Transactional
    public void send(Alarm alarm) {
        context.repository().save(alarm);
        try {
            context.router().route(alarm, context);
            context.repository().save(alarm);
        } catch (SendFailureException exception) {
            context.handleException(alarm, exception);
        }
    }
}
