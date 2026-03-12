package com.payvue.app.presentation.rest.alarm;

public record SystemContent(
    String device,
    String location,
    String ip,
    String reason
) implements AlarmContent {
}
