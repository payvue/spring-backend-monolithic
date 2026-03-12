package com.payvue.app.application.transaction.result;

public record TransferAssetResult(
    String transferId,
    String status
) {}
