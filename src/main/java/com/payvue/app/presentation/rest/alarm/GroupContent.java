package com.payvue.app.presentation.rest.alarm;

public record GroupContent(
    String groupId,
    String groupName,
    String triggerName,
    boolean requiresAction
) implements AlarmContent {
}
