package com.payvue.app.presentation.rest.payment;

public record PaymentReserveRequest(
    String name,
    String type,
    int price,
    int amount,
    String bankCode
) {
}
