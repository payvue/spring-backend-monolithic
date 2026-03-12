package com.payvue.app.application.transaction.command;

import com.payvue.app.domain.community.RecipientType;

public record TransferCashCommand(
    String recipientId,
    RecipientType recipientType,
    Long amount,
    String currency,
    String message,
    String clientRequestId
) {}
