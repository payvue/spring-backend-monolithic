package com.payvue.supporting.interaction;

final class RejectPaymentHandler implements InteractionHandler<ExecutePaymentIntent> {

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof RejectDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends ExecutePaymentIntent> decision) {
    if (decision instanceof RejectDecision rejectDecision) {
      return;
    }
    throw new IllegalArgumentException("결정 타입이 올바르지 않습니다.");
  }
}
