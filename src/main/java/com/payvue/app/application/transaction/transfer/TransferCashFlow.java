package com.payvue.app.application.transaction.transfer;

import com.payvue.app.domain.community.RecipientType;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.CommunityId;
import com.payvue.generic.alarm.AlarmContextManager;

import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.OnlineAssetService;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.application.transaction.TransactionMetaBuilder;
import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.common.status.ExecutionState;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.Actor;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.core.transaction.command.TransactionId;
import com.payvue.core.transaction.command.TransactionRepository;
import com.payvue.core.transaction.command.TransactionService;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.characteristic.HasBalance;
import com.payvue.shared.Money;
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.TransferAssetIntent;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class TransferCashFlow {
  private final TransferCash context;

  private TransferCashFlow(TransferCash context) {
    this.context = context;
  }

  public static TransferCashFlow start(TransferCashCommand command, String idempotencyKey) {
    return new TransferCashFlow(new TransferCash(command, idempotencyKey));
  }

  public TransferCashFlow transaction(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow user(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow community(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow holdings(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow asset(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow interaction(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow notify(Consumer<TransferCashFlow> step) {
    step.accept(this);
    return this;
  }

  public TransferCashFlow checkIdempotency(TransactionRepository transactionRepository) {
    String key = context.idempotencyKey();
    if (key == null || key.isBlank()) {
      return this;
    }
    Optional<Transaction> existing = transactionRepository.findById(new TransactionId(key));
    if (existing.isPresent()) {
      context.transaction(existing.get());
      context.idempotent(true);
    }
    return this;
  }

  public TransferCashFlow loadSender(UserContextManager userContextManager, String email) {
    if (context.idempotent()) {
      return this;
    }
    User sender = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("계정 정보를 찾을 수 없습니다."));
    context.sender(sender);
    return this;
  }

  public TransferCashFlow loadPersonalCommunity(CommunityContextManager communityContextManager) {
    if (context.idempotent()) {
      return this;
    }
    Community community = communityContextManager.resolve(context.sender(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    context.community(community);
    return this;
  }

  public TransferCashFlow loadRecipientCommunity(UserContextManager userContextManager, CommunityContextManager communityContextManager) {
    if (context.idempotent()) {
      return this;
    }
    TransferCashCommand command = context.command();
    String recipientId = command.recipientId();
    if (recipientId == null || recipientId.isBlank()) {
      throw new IllegalArgumentException("수신 커뮤니티를 찾을 수 없습니다.");
    }

    RecipientType recipientType = command.recipientType();

    if (recipientType == RecipientType.COMMUNITY) {

      Community recipient = communityContextManager.get(
              new CategoryQuery(CommunityCategory.COOPERATIVE.name(), CommunityId.of(UUID.fromString(command.recipientId()))));
      context.recipientCommunity(recipient);
    }

    if (recipientType == RecipientType.USER) {
      User recipientUser = userContextManager.findByEmail(command.recipientId(), User.class)
          .orElseGet(() -> userContextManager.findById(
                  UserId.of(UUID.fromString(command.recipientId())),
                  User.class
              ).orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다.")));
      Community recipient = communityContextManager.resolve(recipientUser, CommunityCategory.PERSONAL.name())
          .stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
      context.recipientCommunity(recipient);
    }

    if(Objects.equals(context.community().getId(), context.recipientCommunity().getId())) {
      throw new IllegalArgumentException("대상을 다시 확인해주세요.");
    }

    return this;
  }

  public TransferCashFlow loadHoldings(HoldingsRepository holdingsRepository) {
    if (context.idempotent()) {
      return this;
    }
    Holdings holdings = holdingsRepository.findByCommunity(context.community());
    context.holdings(holdings);
    return this;
  }

  public TransferCashFlow loadRecipientHoldings(HoldingsRepository holdingsRepository) {
    if (context.idempotent()) {
      return this;
    }
    Holdings holdings = holdingsRepository.findByCommunity(context.recipientCommunity());
    context.recipientHoldings(holdings);
    return this;
  }

  public TransferCashFlow loadCashItem(OnlineAssetService onlineAssetService) {
    if (context.idempotent()) {
      return this;
    }
    AssetItem cash = onlineAssetService
        .listOnlineItems(context.community(), AssetClassification.cash(), AssetStatus.ACTIVE)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("온라인 캐시 자산을 조회할 수 있습니댜."));
    context.cashItem(cash);
    return this;
  }

  public TransferCashFlow executeInteraction(InteractionExecutor interactionExecutor) {
    if (context.idempotent()) {
      return this;
    }
    TransferCashCommand command = context.command();
    if (command.amount() == null || command.amount() <= 0L) {
      throw new IllegalArgumentException("전송 금액이 유효하지 않습니다.");
    }
    Money amount = Money.of(command.amount(), "KRW");
    AssetItem sourceItem = context.cashItem();
    HasBalance balance = sourceItem.getCapability(HasBalance.class)
        .orElseThrow(() -> new IllegalArgumentException("잔액 정보를 확인할 수 없습니다."));
    if (balance.balance().isLessThan(amount)) {
      throw new IllegalArgumentException("잔액이 부족합니다.");
    }

    AssetItem transferItem = com.payvue.app.domain.asset.category.Cash.create(
        context.community().getId(),
        context.sender().getId(),
        sourceItem.getName()
    ).with(HasBalance.of(amount));

    Interaction interaction = interactionExecutor.executeInternal(
        new ApproveDecision<>(new TransferAssetIntent(
            Community.to(context.community(), context.sender().getId().getValue()),
            context.holdings(),
            context.recipientHoldings(),
            transferItem
        )),
        context.community()
    );
    if (interaction.isFailed()) {
      throw new IllegalStateException("송금 처리에 실패했습니다.");
    }
    return this;
  }

  public TransferCashFlow saveHoldings(HoldingsRepository holdingsRepository) {
    if (context.idempotent()) {
      return this;
    }
    Transaction transaction = context.transaction();
    if (transaction != null && transaction.interactionState().isCompleted()) {
      holdingsRepository.save(context.holdings());
      holdingsRepository.save(context.recipientHoldings());
    }
    return this;
  }

  public TransferCashFlow process(
      TransactionService transactionService,
      TransactionRepository transactionRepository,
      TransactionMetaBuilder transactionMetaBuilder
  ) {
    if (context.idempotent()) {
      return this;
    }
    TransferCashCommand transferCommand = context.command();
    Money amount = Money.of(transferCommand.amount(), "KRW");
    Actor actor = new Actor(context.community(), context.holdings(), context.sender().getId(), null);
    TransactionMeta meta = transactionMetaBuilder.build(
        context.community(),
        transferCommand.message()
    );
    TransactionCommand txCommand = new TransactionCommand(
        TransactionType.TRANSFER,
        AssetType.CASH.name(),
        context.cashItem(),
        amount,
        actor,
        meta,
        context.community(),
        context.recipientCommunity()
    );
    context.transactionCommand(txCommand);
    Transaction transaction = transactionService.process(txCommand);
    transactionRepository.save(transaction);
    context.transaction(transaction);
    return this;
  }

  public TransferCashFlow sendAlarm(AlarmContextManager alarmChannelRouter) {
    if (context.idempotent()) {
      return this;
    }
    TransferCashCommand command = context.command();
    if (command.recipientId() == null || command.recipientId().isBlank()) {
      return this;
    }
    UserAccountView senderView = UserAccountView.from(context.sender());
    Transaction transaction = context.transaction();
    InAppAlarm alarm = AlarmFactory.createTransferCashAlarm(
        command.recipientId(),
        transaction.getId().toString(),
        command.amount(),
        senderView.displayName()
    );
    alarmChannelRouter.send(alarm);
    return this;
  }

  public TransferCash end() {
    if (context.transaction() == null && context.transactionCommand() != null) {
      context.transaction(Transaction.failed(context.transactionCommand(), ExecutionState.FAILED));
    }
    return context;
  }
}
