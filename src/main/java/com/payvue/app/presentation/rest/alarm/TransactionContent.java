package com.payvue.app.presentation.rest.alarm;

public record TransactionContent(
    Long amount,
    String currency,
    String sender,
    String receiver
) implements AlarmContent {
}
