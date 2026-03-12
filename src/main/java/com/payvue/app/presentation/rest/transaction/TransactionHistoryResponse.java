package com.payvue.app.presentation.rest.transaction;

import com.payvue.app.application.transaction.TransactionHistoryResult;
import java.util.List;

record TransactionHistoryResponse(
    List<Item> items,
    Pagination pagination
) {

  static TransactionHistoryResponse fromResult(TransactionHistoryResult result) {
    if (result == null) {
      return new TransactionHistoryResponse(List.of(), new Pagination(1, 0, 0));
    }
    List<Item> items = result.items() == null
        ? List.of()
        : result.items().stream().map(Item::fromResult).toList();
    Pagination pagination = result.pagination() == null
        ? new Pagination(1, 0, 0)
        : new Pagination(
            result.pagination().page(),
            result.pagination().size(),
            result.pagination().total()
        );
    return new TransactionHistoryResponse(items, pagination);
  }

  record Item(
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
    static Item fromResult(TransactionHistoryResult.Item item) {
      if (item == null) {
        return null;
      }
      return new Item(
          item.id(),
          item.kind(),
          item.amount(),
          item.description(),
          item.message(),
          item.paidBy(),
          item.occurredAt(),
          item.status(),
          item.category(),
          item.platform(),
          item.location(),
          item.time(),
          item.merchantId(),
          item.transactionFee(),
          item.paymentMethod()
      );
    }

    static Item fromAsset(AssetTransactionResponse src) {
      if (src == null) {
        return null;
      }
      return new Item(
          src.id(),
          src.type(),
          src.amount(),
          src.assetName(),
          null,
          null,
          src.date(),
          src.status(),
          src.category(),
          null,
          null,
          null,
          null,
          null,
          null
      );
    }

    static Item fromRecent(RecentTransactionResponse src) {
      if (src == null) {
        return null;
      }
      return new Item(
          src.id(),
          src.type(),
          src.amount(),
          src.description(),
          null,
          null,
          src.occurredAt(),
          src.status(),
          src.category(),
          null,
          null,
          null,
          null,
          null,
          null
      );
    }

    static Item fromGroup(GroupTransactionResponse src) {
      if (src == null) {
        return null;
      }
      return new Item(
          src.id(),
          src.type(),
          src.amount(),
          src.description(),
          null,
          src.paidBy(),
          src.date(),
          src.status(),
          src.category(),
          null,
          null,
          null,
          null,
          null,
          null
      );
    }
  }

  record Pagination(
      int page,
      int size,
      long total
  ) {}
}
