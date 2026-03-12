package com.payvue.supporting.interaction;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Coupon;
import com.payvue.app.domain.asset.category.GameItem;
import com.payvue.app.domain.asset.category.GiftCard;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.app.domain.asset.category.MusicItem;
import com.payvue.app.domain.asset.category.ShoppingItem;
import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.UserId;
import java.time.Instant;
import java.util.UUID;

final class ApprovePaymentRequestHandler implements InteractionHandler<PaymentRequestIntent> {

  private final PaymentRequestRepository paymentRequestRepository;
  private final HoldingsRepository holdingsRepository;
  private final InteractionExecutor interactionExecutor;

  ApprovePaymentRequestHandler(
      PaymentRequestRepository paymentRequestRepository,
      HoldingsRepository holdingsRepository,
      InteractionExecutor interactionExecutor
  ) {
    this.paymentRequestRepository = paymentRequestRepository;
    this.holdingsRepository = holdingsRepository;
    this.interactionExecutor = interactionExecutor;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof ApproveDecision;
  }

  @Override
  public void handle(
      InteractionContext context,
      Decision<? extends PaymentRequestIntent> decision
  ) {
    if (!(decision instanceof ApproveDecision)) {
      throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
    }
    if (!(context instanceof InternalInteractionContext internal)) {
      throw new IllegalArgumentException("내부 컨텍스트가 필요합니다.");
    }
    PaymentRequest request = resolveRequest(decision.getIntent());
    Community community = internal.getCommunity();
    Holdings holdings = holdingsRepository.findByCommunity(community);
    AssetItem paymentItem = buildPaymentItem(
        request,
        community,
        UserId.of(decision.getIntent().initiator().getUserId())
    );
    Interaction interaction = interactionExecutor.executeInternal(
        new ApproveDecision<>(new ExecutePaymentIntent(
            decision.getIntent().initiator(),
            holdings,
            paymentItem
        )),
        community
    );
    if (!interaction.isCompleted()) {
      throw new IllegalStateException("결제 승인 처리에 실패했습니다.");
    }
    holdingsRepository.save(holdings);
    request.approve(Instant.now());
    paymentRequestRepository.save(request);
  }

  private PaymentRequest resolveRequest(PaymentRequestIntent intent) {
    String requestId = intent.requestId();
    if (requestId == null || requestId.isBlank()) {
      throw new IllegalArgumentException("요청 ID가 필요합니다.");
    }
    return paymentRequestRepository.findById(
        PaymentRequestId.of(UUID.fromString(requestId))
    ).orElseThrow(() -> new IllegalArgumentException("결제 요청을 찾을 수 없습니다."));
  }

  private AssetItem buildPaymentItem(
      PaymentRequest request,
      Community community,
      UserId contributorId
  ) {
    String name = request.description();
    if (name == null || name.isBlank()) {
      name = "payment-request";
    }
    AssetClassification classification = AssetClassification.of(request.category());
    AssetItem item = switch (classification.getCategory()) {
      case CASH -> Cash.create(community.getId(), contributorId, name);
      case MILEAGE -> Mileage.create(community.getId(), contributorId, name);
      case GIFT -> GiftCard.create(community.getId(), contributorId, name);
      case GAME -> GameItem.create(community.getId(), contributorId, name);
      case MUSIC -> MusicItem.create(community.getId(), contributorId, name);
      case SHOPPING -> ShoppingItem.create(community.getId(), contributorId, name);
      case COUPON -> Coupon.create(community.getId(), contributorId, name);
    };
    item.with(HasBalance.of(Money.of(request.amount(), "KRW")));
    return item;
  }
}
