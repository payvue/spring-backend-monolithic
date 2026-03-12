package com.payvue.app.presentation.rest.alarm;

import java.util.List;

public record AlarmNotificationList(
    List<AlarmNotification> noti
) {
}
