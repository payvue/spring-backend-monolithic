package com.payvue.core.transaction.command;

import com.payvue.common.status.ExecutionState;

final class CommisionTransactionHandler implements TransactionHandler {

  @Override
  public Transaction handle(TransactionCommand command) {
    return Transaction.failed(command, ExecutionState.FAILED);
  }
}
