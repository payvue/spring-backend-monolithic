package com.payvue.supporting.interaction;

import com.payvue.core.community.Participant;
import java.time.Instant;

public record PaymentRequestCreateIntent(
    Participant initiator,
    String requestId,
    String groupId,
    long amount,
    String description,
    String category,
    String urgency,
    Instant requestedAt
) implements Intent {

  @Override
  public String getCategory() {
    return IntentCategory.PAYMENT_REQUEST.name();
  }
}
