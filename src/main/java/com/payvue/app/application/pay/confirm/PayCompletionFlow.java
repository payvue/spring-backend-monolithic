package com.payvue.app.application.pay.confirm;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.presentation.rest.community.SystemCommunityProvider;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.payment.PaymentService;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.Actor;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionCommand;
import com.payvue.core.transaction.command.TransactionService;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentRepository;
import com.payvue.core.asset.AssetItem;
import com.payvue.generic.payment.PaymentStatus;
import com.payvue.shared.Money;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.User;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class PayCompletionFlow {
  private final PayCompletion context;

  private PayCompletionFlow(PayCompletion context) {
    this.context = context;
  }

  public static PayCompletionFlow start(
      String payId,
      String resultCode,
      String reserveId,
      String paymentId
  ) {
    return new PayCompletionFlow(new PayCompletion(payId, resultCode, reserveId, paymentId));
  }

  public PayCompletionFlow payment(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow transaction(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow notify(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow community(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow user(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow asset(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow save(Consumer<PayCompletionFlow> step) {
    step.accept(this);
    return this;
  }

  public PayCompletionFlow confirmPayment(PaymentService paymentService) {
    String gatewayPaymentId = context.paymentId();
    if (gatewayPaymentId == null || gatewayPaymentId.isBlank()) {
      throw new IllegalArgumentException("paymentId가 없습니다.");
    }
    Payment confirmed = paymentService.confirm(context.payId(), gatewayPaymentId);
    context.payment(confirmed);
    return this;
  }

  public PayCompletionFlow loadCommunity(CommunityContextManager communityContextManager) {
    String communityId = requiredMetadata("communityId");
    Community community = communityContextManager.get(new CategoryQuery(
        CommunityCategory.PERSONAL.name(),
        CommunityId.of(UUID.fromString(communityId))
    ));
    context.community(community);
    return this;
  }

  public PayCompletionFlow loadOwner(UserContextManager userContextManager) {
    String email = requiredMetadata("ownerEmail");
    User owner = userContextManager.findByEmail(email, User.class)
        .orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));
    context.owner(owner);
    return this;
  }

  public PayCompletionFlow rebuildOrderItemFromMetadata() {
    String orderId = requiredMetadata("orderId");
    String name = requiredMetadata("orderName");
    String quantity = requiredMetadata("orderQuantity");
    String unitPrice = requiredMetadata("orderUnitPrice");
    OrderItem order = new OrderItem(
        UUID.fromString(orderId),
        name,
        Integer.parseInt(quantity),
        Money.of(new BigDecimal(unitPrice), "KRW")
    );
    context.order(order);
    return this;
  }

  public PayCompletionFlow loadHoldings(HoldingsRepository holdingsRepository) {
    Holdings holdings = holdingsRepository.findByCommunity(context.community());
    context.holdings(holdings);
    return this;
  }

  public PayCompletionFlow acquire(
      TransactionService transactionService,
      SystemCommunityProvider systemCommunityProvider
  ) {

    if(Objects.equals(context.payment().status(), PaymentStatus.APPROVED)) {
      AssetItem cash = Cash.create(
              context.community().getId(),
              context.owner().getId(),
              context.order().name()
      ).with(HasBalance.of(context.order().totalPrice()));

      Actor actor = new Actor(context.community(), context.holdings(), context.owner().getId(), null);
      com.payvue.core.community.Community system = systemCommunityProvider.getOrCreate();
      TransactionCommand command = new TransactionCommand(
              TransactionType.ACQUIRE,
              AssetType.CASH.name(),
              cash,
              context.order().totalPrice(),
              actor,
              TransactionMeta.empty(),
              system,
              context.community()
      );
      Transaction transaction = transactionService.process(command);
      context.transaction(transaction);
    }
    return this;
  }

  public PayCompletionFlow send(AlarmContextManager alarmChannelRouter) {

    if(context.transaction().interactionState().isCompleted()) {
      Transaction transaction = context.transaction();
      if (transaction == null || transaction.interactionState() == null
              || !transaction.interactionState().isCompleted()) {
        return this;
      }
      UserAccountView ownerView = UserAccountView.from(context.owner());
      InAppAlarm alarm = AlarmFactory.createBalanceChargeAlarm(
              context.owner().getId().getValue().toString(),
              transaction.getId().toString(),
              context.order().totalPrice().toLong(),
              ownerView.displayName(),
              "결제"
      );
      alarmChannelRouter.send(alarm);
    }

    return this;
  }

  public PayCompletionFlow persist(PaymentRepository paymentRepository) {
    paymentRepository.save(context.payment());
    return this;
  }

  public PayCompletion end() {
    return context;
  }

  private String requiredMetadata(String key) {
    if (context.payment() == null || context.payment().metadata() == null) {
      throw new IllegalStateException("결제 메타데이터가 없습니다.");
    }
    String value = context.payment().metadata().get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("결제 메타데이터가 누락되었습니다: " + key);
    }
    return value;
  }
}
