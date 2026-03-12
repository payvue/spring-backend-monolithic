package com.payvue.supporting.interaction;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import java.time.Instant;
import java.util.UUID;

final class RejectPaymentRequestHandler implements InteractionHandler<PaymentRequestIntent> {

  private final PaymentRequestRepository paymentRequestRepository;

  RejectPaymentRequestHandler(PaymentRequestRepository paymentRequestRepository) {
    this.paymentRequestRepository = paymentRequestRepository;
  }

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof RejectDecision;
  }

  @Override
  public void handle(
      InteractionContext context,
      Decision<? extends PaymentRequestIntent> decision
  ) {
    if (!(decision instanceof RejectDecision rejectDecision)) {
      throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
    }
    PaymentRequest request = resolveRequest(decision.getIntent());
    request.reject(rejectDecision.getReason(), Instant.now());
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
}
