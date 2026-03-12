package com.payvue.supporting.interaction;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.core.community.Community;
import com.payvue.core.hold.HoldingsRepository;
import java.util.Objects;
import java.util.UUID;

class PaymentRequestDomainService implements DomainService<Intent> {

  private final PaymentRequestRepository paymentRequestRepository;
  private final HoldingsRepository holdingsRepository;

  private PaymentRequestDomainService(
      PaymentRequestRepository paymentRequestRepository,
      HoldingsRepository holdingsRepository
  ) {
    this.paymentRequestRepository = Objects.requireNonNull(
        paymentRequestRepository,
        "paymentRequestRepository는 필수입니다"
    );
    this.holdingsRepository = Objects.requireNonNull(
        holdingsRepository,
        "holdingsRepository는 필수입니다"
    );
  }

  public static DomainService<Intent> of(
      PaymentRequestRepository paymentRequestRepository,
      HoldingsRepository holdingsRepository
  ) {
    return new PaymentRequestDomainService(paymentRequestRepository, holdingsRepository);
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision.getIntent().getCategory() == IntentCategory.PAYMENT_REQUEST.name();
  }

  @Override
  public Interaction execute(Decision<Intent> decision) {
    throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
  }

  @Override
  public Interaction execute(
      InteractionContext context,
      Decision<Intent> decision,
      InteractionHandler<Intent> handler
  ) {
    Interaction interaction = Interaction.pending(decision);
    if (!(context instanceof InternalInteractionContext)) {
      throw new IllegalArgumentException("내부 컨텍스트가 필요합니다.");
    }
    if (decision.getIntent() instanceof PaymentRequestCreateIntent) {
      handler.handle(context, decision);
      interaction.complete();
      return interaction;
    }

    if (decision instanceof ApproveDecision) {
      interaction.approve();
      handler.handle(context, decision);
      return interaction;
    }
    if (decision instanceof RejectDecision rejectDecision) {
      interaction.reject(rejectDecision.getReason());
      handler.handle(context, decision);
      return interaction;
    }
    throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
  }

}
