package com.payvue.core.transaction.query;

public record TransactionHistoryView(
    CoreInfo core,
    PaymentInfo payment,
    ContextInfo context,
    FeeInfo fee
) {

  public record CoreInfo(
      String id,
      String kind,
      Long amount,
      String description,
      String message,
      String paidBy,
      String occurredAt,
      String status,
      String category
  ) {}

  public record PaymentInfo(
      String merchantId,
      String paymentMethodId
  ) {}

  public record ContextInfo(
      String platform,
      String location,
      String time
  ) {}

  public record FeeInfo(
      Long transactionFee
  ) {}
}
