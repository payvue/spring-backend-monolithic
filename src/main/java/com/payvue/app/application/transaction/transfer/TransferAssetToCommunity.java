package com.payvue.app.application.transaction.transfer;

import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.user.command.User;

public final class TransferAssetToCommunity {
  private final TransferAssetToCommunityCommand command;
  private final AssetType assetType;
  private User sender;
  private Community senderCommunity;
  private Community targetCommunity;
  private Holdings senderHoldings;
  private Holdings targetHoldings;
  private AssetItem assetItem;
  private long amount;
  private Transaction transaction;

  public TransferAssetToCommunity(TransferAssetToCommunityCommand command, AssetType assetType) {
    this.command = command;
    this.assetType = assetType;
  }

  public TransferAssetToCommunityCommand command() {
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

  public Community senderCommunity() {
    return senderCommunity;
  }

  public void senderCommunity(Community senderCommunity) {
    this.senderCommunity = senderCommunity;
  }

  public Community targetCommunity() {
    return targetCommunity;
  }

  public void targetCommunity(Community targetCommunity) {
    this.targetCommunity = targetCommunity;
  }

  public Holdings senderHoldings() {
    return senderHoldings;
  }

  public void senderHoldings(Holdings senderHoldings) {
    this.senderHoldings = senderHoldings;
  }

  public Holdings targetHoldings() {
    return targetHoldings;
  }

  public void targetHoldings(Holdings targetHoldings) {
    this.targetHoldings = targetHoldings;
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
