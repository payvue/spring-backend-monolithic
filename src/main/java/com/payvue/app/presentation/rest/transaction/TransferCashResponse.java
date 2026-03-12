package com.payvue.app.presentation.rest.transaction;

record TransferCashResponse(
    String transferId,
    String status,
    Long balance
) {}
