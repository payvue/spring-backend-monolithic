package com.payvue.core.transaction.command;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.command.meta.TransactionMetaKey;
import com.payvue.shared.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

final class CommissionTransactionDecorator implements TransactionDecorator {

  private static final Money DEFAULT_COMMISSION = Money.won(500L);

  @Override
  public Transaction decorate(Transaction transaction) {
    Objects.requireNonNull(transaction, "transaction은 필수입니다.");
    HasBalance balanceCapability = transaction.item()
        .getCapability(HasBalance.class)
        .orElseThrow(() -> new IllegalStateException("수수료 계산을 위한 balance capability가 필요합니다."));

    if(balanceCapability.balance().isLessThan(DEFAULT_COMMISSION)) {
      throw new IllegalStateException("수수료를 납부할 금액이 부족합니다.");
    }

    balanceCapability.deduct(DEFAULT_COMMISSION);

    TransactionMeta updated = transaction.meta().merge(
        TransactionMeta.of(Map.of(
            TransactionMetaKey.COMMISSION,
            DEFAULT_COMMISSION.getAmount().toPlainString()
        ))
    );

    return Transaction.reconstitute(
        transaction.getId(),
        transaction.type(),
        transaction.assetType(),
        transaction.from(),
        transaction.to(),
        transaction.item(),
        transaction.amount(),
        transaction.actorId(),
        transaction.description(),
        transaction.category(),
        transaction.interactionState(),
        transaction.occurredAt(),
        updated
    );
  }
}
