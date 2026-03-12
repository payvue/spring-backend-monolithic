package com.payvue.app.application.transaction.command;

public record TransferAssetToUserCommand(
    String assetId,
    String recipientEmail,
    String message,
    String clientRequestId
) {}
