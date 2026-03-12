package com.payvue.app.infrastructure.pg.toss;

record TossVirtualAccountRequest(
    String method,
    String bank,
    long amount,
    String orderId,
    String orderName,
    String customerName,
    Integer validHours
) {
  static TossVirtualAccountRequest create(
      String bank,
      long amount,
      String orderId,
      String orderName,
      String customerName,
      Integer validHours
  ) {
    return new TossVirtualAccountRequest(
        "VIRTUAL_ACCOUNT",
        bank,
        amount,
        orderId,
        orderName,
        customerName,
        validHours
    );
  }
}
