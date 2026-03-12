package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.core.community.Community;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RejectPaymentHandlerTest {

  @Test
  @DisplayName("거절 결정은 예외 없이 처리된다")
  void rejectDecisionSucceeds() {
    RejectPaymentHandler handler = new RejectPaymentHandler();
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        com.payvue.core.community.Participant.of(UUID.randomUUID(), Role.ADMIN),
        null,
        null
    );
    RejectDecision<ExecutePaymentIntent> decision = new RejectDecision<>(intent, "reason");
    Community community = Cooperative.create("org", "desc", UUID.randomUUID(), Role.ADMIN, Role.MEMBER);

    assertThatCode(() -> handler.handle(new InternalInteractionContext(community), decision))
            .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("거절 결정이 아니면 예외가 발생한다")
  void rejectThrowsWhenDecisionTypeMismatch() {
    RejectPaymentHandler handler = new RejectPaymentHandler();
    ExecutePaymentIntent intent = new ExecutePaymentIntent(
        com.payvue.core.community.Participant.of(UUID.randomUUID(), Role.ADMIN),
        null,
        null
    );
    ApproveDecision<ExecutePaymentIntent> decision = new ApproveDecision<>(intent);
    Community community = Cooperative.create("org", "desc", UUID.randomUUID(), Role.ADMIN, Role.MEMBER);

    assertThatThrownBy(() -> handler.handle(new InternalInteractionContext(community), decision))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
