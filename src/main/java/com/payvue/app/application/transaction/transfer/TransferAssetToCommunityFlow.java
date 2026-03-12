package com.payvue.app.application.transaction.transfer;

import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.application.transaction.TransactionMetaBuilder;
import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand;
import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.characteristic.Consumable;
import com.payvue.common.status.ExecutionState;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.CommunityRepository;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.specification.IdSpecification;
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
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.TransferAssetIntent;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.UUID;
import java.util.function.Consumer;

public final class TransferAssetToCommunityFlow {
  private final TransferAssetToCommunity context;

  private TransferAssetToCommunityFlow(TransferAssetToCommunity context) {
    this.context = context;
  }

  public static TransferAssetToCommunityFlow start(TransferAssetToCommunityCommand command, AssetType assetType) {
    return new TransferAssetToCommunityFlow(new TransferAssetToCommunity(command, assetType));
  }

  public TransferAssetToCommunityFlow user(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow community(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow holdings(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow asset(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow transaction(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow notify(Consumer<TransferAssetToCommunityFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferAssetToCommunityFlow loadSender(UserContextManager userContextManager, String email) {
    User sender = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("계정 정보를 찾을 수 없습니다."));
    context.sender(sender);
    return this;
  }

  public TransferAssetToCommunityFlow loadCommunities(
      CommunityContextManager communityContextManager,
      CommunityRepository communityRepository
  ) {
    Community senderCommunity = communityContextManager.resolve(context.sender(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    Community targetCommunity = communityRepository.findById(
        CommunityId.of(UUID.fromString(context.command().groupId()))
    ).orElseThrow(() -> new IllegalArgumentException("커뮤니티를 찾을 수 없습니다."));
    context.senderCommunity(senderCommunity);
    context.targetCommunity(targetCommunity);
    return this;
  }

  public TransferAssetToCommunityFlow loadHoldings(HoldingsRepository holdingsRepository) {
    Holdings senderHoldings = holdingsRepository.findByCommunity(context.senderCommunity());
    Holdings targetHoldings = holdingsRepository.findByCommunity(context.targetCommunity());
    context.senderHoldings(senderHoldings);
    context.targetHoldings(targetHoldings);
    return this;
  }

  public TransferAssetToCommunityFlow resolveAssetItem() {
    AssetItem assetItem = findAssetItem(context.senderHoldings(), context.command().assetId());
    context.assetItem(assetItem);
    context.amount(resolveTransferAmount(assetItem));
    return this;
  }

  public TransferAssetToCommunityFlow process(
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
        context.targetCommunity()
    );

    Transaction transaction = transactionService.process(command);
    transactionRepository.save(transaction);
    context.transaction(transaction);
    return this;
  }

  public TransferAssetToCommunityFlow sendAlarm(AlarmContextManager alarmChannelRouter) {
    Transaction transaction = context.transaction();
    if (transaction != null && context.transaction().interactionState().isCompleted()) {
      UserAccountView senderView = UserAccountView.from(context.sender());
      Trigger trigger = new Trigger(
              senderView.displayName(),
              context.senderCommunity().getParticipantRole(context.sender().getId().getValue()).getName()
      );
      for (UUID participantId : context.targetCommunity().getParticipantIds()) {
        if (participantId.equals(context.sender().getId().getValue())) {
          continue;
        }
        InAppAlarm alarm = AlarmFactory.createGroupTransferAlarm(
                participantId.toString(),
                context.targetCommunity().getId().getValue().toString(),
                context.targetCommunity().getName(),
                trigger,
                context.targetCommunity().getName(),
                context.amount()
        );
        alarmChannelRouter.send(alarm);
      }
    }
    return this;
  }

  public TransferAssetToCommunity end() {
    return context;
  }

  private AssetItem findAssetItem(Holdings holdings, String assetId) {
    if (holdings == null || assetId == null || assetId.isBlank()) {
      throw new IllegalArgumentException("자산 정보를 확인할 수 없습니다.");
    }
    return holdings.ownedSingleAssetsConformingTo(new IdSpecification(AssetItemId.of(assetId)))
        .orElseThrow(() -> new IllegalArgumentException("자산을 찾을 수 없습니다."));
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
