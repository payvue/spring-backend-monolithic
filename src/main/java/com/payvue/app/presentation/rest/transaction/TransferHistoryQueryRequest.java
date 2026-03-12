package com.payvue.app.presentation.rest.transaction;

record TransferHistoryQueryRequest(
    Integer page,
    Integer size,
    String fromDate,
    String toDate
) {}
