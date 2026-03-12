package com.payvue.supporting.interaction;

import com.payvue.core.community.Participant;

public record PaymentRequestIntent(
    Participant initiator,
    String requestId,
    String groupId
) implements Intent, RequiresApproval {

  @Override
  public String getCategory() {
    return IntentCategory.PAYMENT_REQUEST.name();
  }
}
