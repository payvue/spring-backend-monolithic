package com.payvue.app.application.transaction.transfer;

import com.payvue.app.domain.asset.specification.IdSpecification;
import com.payvue.core.asset.AssetItemId;
import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.OnlineAssetService;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.application.transaction.TransactionMetaBuilder;
import com.payvue.app.application.transaction.command.TransferAssetToUserCommand;
import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.characteristic.Consumable;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.Actor;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.core.transaction.command.TransactionRepository;
import com.payvue.core.transaction.command.TransactionService;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.generic.alarm.Trigger;
import com.payvue.shared.Money;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.util.function.Consumer;

public final class TransferAssetToUserFlow {
  private final TransferAssetToUser context;

  private TransferAssetToUserFlow(TransferAssetToUser context) {
    this.context = context;
  }

  public static TransferAssetToUserFlow start(TransferAssetToUserCommand command, AssetType assetType) {
    return new TransferAssetToUserFlow(new TransferAssetToUser(command, assetType));
  }

  public TransferAssetToUserFlow user(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow community(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow holdings(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow asset(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow transaction(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow notify(Consumer<TransferAssetToUserFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToUserFlow loadSender(UserContextManager userContextManager, String email) {
    User sender = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("계정 정보를 찾을 수 없습니다."));
    context.sender(sender);
    return this;
  }

  public TransferAssetToUserFlow loadRecipient(UserContextManager userContextManager) {
    String email = context.command().recipientEmail();
    User recipient = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));
    context.recipient(recipient);
    return this;
  }

  public TransferAssetToUserFlow loadPersonalCommunities(CommunityContextManager communityContextManager) {
    Community senderCommunity = communityContextManager.resolve(context.sender(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    Community recipientCommunity = communityContextManager.resolve(context.recipient(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    context.senderCommunity(senderCommunity);
    context.recipientCommunity(recipientCommunity);
    return this;
  }

  public TransferAssetToUserFlow loadHoldings(HoldingsRepository holdingsRepository) {
    Holdings senderHoldings = holdingsRepository.findByCommunity(context.senderCommunity());
    Holdings recipientHoldings = holdingsRepository.findByCommunity(context.recipientCommunity());
    context.senderHoldings(senderHoldings);
    context.recipientHoldings(recipientHoldings);
    return this;
  }

  public TransferAssetToUserFlow resolveAssetItem(OnlineAssetService onlineAssetService) {

    AssetItem assetItem = context.senderHoldings()
            .ownedAssetsConformingTo(new IdSpecification(AssetItemId.of(context.command().assetId())))
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));

    context.assetItem(assetItem);
    context.amount(resolveTransferAmount(assetItem));
    return this;
  }

  public TransferAssetToUserFlow process(
      TransactionService transactionService,
      TransactionRepository transactionRepository,
      TransactionMetaBuilder transactionMetaBuilder
  ) {
    TransactionMeta meta = transactionMetaBuilder.build(
        context.senderCommunity(),
        context.command().message()
    );
    TransactionCommand command = new TransactionCommand(
        TransactionType.TRANSFER,
        context.assetType().name(),
        context.assetItem(),
        Money.of(context.amount(), "KRW"),
        new Actor(context.senderCommunity(), context.senderHoldings(), context.sender().getId(), null),
        meta,
        context.senderCommunity(),
        context.recipientCommunity()
    );

    Transaction transaction = transactionService.process(command);
    transactionRepository.save(transaction);
    context.transaction(transaction);
    return this;
  }

  public TransferAssetToUserFlow sendAlarm(AlarmContextManager alarmChannelRouter) {

    Transaction transaction = context.transaction();

    if (transaction != null && transaction.interactionState().isCompleted()) {

      UserAccountView senderView = UserAccountView.from(context.sender());
      UserAccountView recipientView = UserAccountView.from(context.recipient());

      InAppAlarm alarm = AlarmFactory.createGroupTransferAlarm(
              context.recipient().getId().getValue().toString(),
              context.recipientCommunity().getId().getValue().toString(),
              context.recipientCommunity().getName(),
              new Trigger(
                      senderView.displayName(),
                      context.senderCommunity().getParticipantRole(context.sender().getId().getValue()).getName()
              ),
              recipientView.displayName(),
              context.amount()
      );
      alarmChannelRouter.send(alarm);
    }
    return this;
  }

  public TransferAssetToUser end() {
    return context;
  }

  private long resolveTransferAmount(AssetItem assetItem) {
    if (assetItem == null || assetItem.getCapabilities() == null) {
      return 1L;
    }
    for (AssetCharacteristic capability : assetItem.getCapabilities()) {
      if (capability instanceof Consumable consumable) {
        return Math.max(1L, consumable.quantity());
      }
    }
    return 1L;
  }
}
