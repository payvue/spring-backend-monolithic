package com.payvue.app.presentation.rest.alarm;

public record AlarmEvent(
    String type,
    AlarmMeta meta,
    AlarmContent content
) {
}
