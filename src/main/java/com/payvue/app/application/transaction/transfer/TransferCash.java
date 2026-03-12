package com.payvue.app.application.transaction.transfer;

import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.supporting.user.command.User;

public final class TransferCash {
  private final TransferCashCommand command;
  private final String idempotencyKey;
  private User sender;
  private Community community;
  private Holdings holdings;
  private AssetItem cashItem;
  private Community recipientCommunity;
  private Holdings recipientHoldings;
  private TransactionCommand transactionCommand;
  private Transaction transaction;
  private boolean idempotent;

  public TransferCash(TransferCashCommand command, String idempotencyKey) {
    this.command = command;
    this.idempotencyKey = idempotencyKey;
  }

  public TransferCashCommand command() {
    return command;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public User sender() {
    return sender;
  }

  public void sender(User sender) {
    this.sender = sender;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public Holdings holdings() {
    return holdings;
  }

  public void holdings(Holdings holdings) {
    this.holdings = holdings;
  }

  public AssetItem cashItem() {
    return cashItem;
  }

  public void cashItem(AssetItem cashItem) {
    this.cashItem = cashItem;
  }

  public Community recipientCommunity() {
    return recipientCommunity;
  }

  public void recipientCommunity(Community recipientCommunity) {
    this.recipientCommunity = recipientCommunity;
  }

  public Holdings recipientHoldings() {
    return recipientHoldings;
  }

  public void recipientHoldings(Holdings recipientHoldings) {
    this.recipientHoldings = recipientHoldings;
  }

  public TransactionCommand transactionCommand() {
    return transactionCommand;
  }

  public void transactionCommand(TransactionCommand transactionCommand) {
    this.transactionCommand = transactionCommand;
  }

  public Transaction transaction() {
    return transaction;
  }

  public void transaction(Transaction transaction) {
    this.transaction = transaction;
  }

  public boolean idempotent() {
    return idempotent;
  }

  public void idempotent(boolean idempotent) {
    this.idempotent = idempotent;
  }
}
