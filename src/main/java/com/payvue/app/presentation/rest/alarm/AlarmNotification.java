package com.payvue.app.presentation.rest.alarm;

public record AlarmNotification(
    String id,
    String title,
    String message,
    String createdAt,
    AlarmEvent event
) {
}
