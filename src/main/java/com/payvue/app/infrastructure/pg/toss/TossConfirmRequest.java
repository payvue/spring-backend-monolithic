package com.payvue.app.infrastructure.pg.toss;

record TossConfirmRequest(
    String paymentKey,
    String orderId,
    long amount
) { }
