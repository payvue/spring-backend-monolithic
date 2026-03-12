package com.payvue.app.application.transaction.transfer;

import com.payvue.app.application.transaction.command.TransferAssetToUserCommand;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.user.command.User;

public final class TransferAssetToUser {
  private final TransferAssetToUserCommand command;
  private final AssetType assetType;
  private User sender;
  private User recipient;
  private Community senderCommunity;
  private Community recipientCommunity;
  private Holdings senderHoldings;
  private Holdings recipientHoldings;
  private AssetItem assetItem;
  private long amount;
  private Transaction transaction;

  public TransferAssetToUser(TransferAssetToUserCommand command, AssetType assetType) {
    this.command = command;
    this.assetType = assetType;
  }

  public TransferAssetToUserCommand command() {
    return command;
  }

  public AssetType assetType() {
    return assetType;
  }

  public User sender() {
    return sender;
  }

  public void sender(User sender) {
    this.sender = sender;
  }

  public User recipient() {
    return recipient;
  }

  public void recipient(User recipient) {
    this.recipient = recipient;
  }

  public Community senderCommunity() {
    return senderCommunity;
  }

  public void senderCommunity(Community senderCommunity) {
    this.senderCommunity = senderCommunity;
  }

  public Community recipientCommunity() {
    return recipientCommunity;
  }

  public void recipientCommunity(Community recipientCommunity) {
    this.recipientCommunity = recipientCommunity;
  }

  public Holdings senderHoldings() {
    return senderHoldings;
  }

  public void senderHoldings(Holdings senderHoldings) {
    this.senderHoldings = senderHoldings;
  }

  public Holdings recipientHoldings() {
    return recipientHoldings;
  }

  public void recipientHoldings(Holdings recipientHoldings) {
    this.recipientHoldings = recipientHoldings;
  }

  public AssetItem assetItem() {
    return assetItem;
  }

  public void assetItem(AssetItem assetItem) {
    this.assetItem = assetItem;
  }

  public long amount() {
    return amount;
  }

  public void amount(long amount) {
    this.amount = amount;
  }

  public Transaction transaction() {
    return transaction;
  }

  public void transaction(Transaction transaction) {
    this.transaction = transaction;
  }
}
