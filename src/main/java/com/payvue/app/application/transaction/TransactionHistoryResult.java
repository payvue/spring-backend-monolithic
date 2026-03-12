package com.payvue.app.application.transaction;

import com.payvue.core.transaction.query.TransactionHistoryView;
import java.util.List;

public record TransactionHistoryResult(
    List<Item> items,
    Pagination pagination
) {

  public static TransactionHistoryResult fromViews(
      List<TransactionHistoryView> views,
      Pagination pagination
  ) {
    List<Item> items = views == null
        ? List.of()
        : views.stream().map(Item::fromView).toList();
    return new TransactionHistoryResult(items, pagination);
  }

  public record Item(
      String id,
      String kind,
      Long amount,
      String description,
      String message,
      String paidBy,
      String occurredAt,
      String status,
      String category,
      String platform,
      String location,
      String time,
      String merchantId,
      Long transactionFee,
      String paymentMethod
  ) {
    static Item fromView(TransactionHistoryView view) {
      if (view == null) {
        return null;
      }
      TransactionHistoryView.CoreInfo core = view.core();
      TransactionHistoryView.PaymentInfo payment = view.payment();
      TransactionHistoryView.ContextInfo context = view.context();
      TransactionHistoryView.FeeInfo fee = view.fee();

      return new Item(
          core == null ? null : core.id(),
          core == null ? null : core.kind(),
          core == null ? null : core.amount(),
          core == null ? null : core.description(),
          core == null ? null : core.message(),
          core == null ? null : core.paidBy(),
          core == null ? null : core.occurredAt(),
          core == null ? null : core.status(),
          core == null ? null : core.category(),
          context == null ? null : context.platform(),
          context == null ? null : context.location(),
          context == null ? null : context.time(),
          payment == null ? null : payment.merchantId(),
          fee == null ? null : fee.transactionFee(),
          payment == null ? null : payment.paymentMethodId()
      );
    }
  }

  public record Pagination(
      int page,
      int size,
      long total
  ) {}
}
