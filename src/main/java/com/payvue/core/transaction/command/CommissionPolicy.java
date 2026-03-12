package com.payvue.core.transaction.command;

import java.util.Objects;

final class CommissionPolicy implements TransactionPolicy {

  private final String assetType;
  private final TransactionType baseType;

  public CommissionPolicy(String assetType, TransactionType baseType) {
    Objects.requireNonNull(assetType, "assetType은 필수입니다.");
    this.assetType = assetType.toUpperCase(java.util.Locale.ROOT);
    this.baseType = Objects.requireNonNull(baseType, "baseType은 필수입니다.");
  }

  @Override
  public boolean shouldApply(TransactionCommand command, Transaction primaryResult) {
    if (primaryResult.interactionState() == null || !primaryResult.interactionState().isCompleted()) {
      return false;
    }
    return command.assetType().equals(assetType) && command.type() == baseType;
  }
}
