package com.payvue.app.application.transaction.command;

public record TransferAssetToCommunityCommand(
    String assetId,
    String groupId,
    String message,
    String clientRequestId
) {}
