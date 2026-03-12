package com.payvue.supporting.interaction;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import java.util.UUID;

final class CreatePaymentRequestHandler implements InteractionHandler<PaymentRequestCreateIntent> {

  private final PaymentRequestRepository paymentRequestRepository;

  CreatePaymentRequestHandler(PaymentRequestRepository paymentRequestRepository) {
    this.paymentRequestRepository = paymentRequestRepository;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof CreatePaymentRequestDecision;
  }

  @Override
  public void handle(
      InteractionContext context,
      Decision<? extends PaymentRequestCreateIntent> decision
  ) {
    if (!(decision instanceof CreatePaymentRequestDecision createDecision)) {
      throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
    }
    PaymentRequestCreateIntent intent = createDecision.getIntent();
    PaymentRequest request = PaymentRequest.createWithId(
        PaymentRequestId.of(UUID.fromString(intent.requestId())),
        intent.groupId(),
        intent.initiator().getUserId().toString(),
        intent.amount(),
        intent.description(),
        intent.category(),
        intent.urgency(),
        intent.requestedAt()
    );
    paymentRequestRepository.save(request);
  }
}
