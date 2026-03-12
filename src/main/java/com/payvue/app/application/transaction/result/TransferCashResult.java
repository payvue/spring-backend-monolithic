package com.payvue.app.application.transaction.result;

public record TransferCashResult(
    String transferId,
    String status,
    Long balance
) {}
