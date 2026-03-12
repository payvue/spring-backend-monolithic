package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberManagementDomainServiceTest {

  @Test
  @DisplayName("관리 결정은 대상이 initiator와 다를 때 핸들러가 실행된다")
  void executeRunsHandlerWhenInitiatorIsParticipant() {
    UUID adminId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );

    Participant initiator = Participant.of(adminId, Role.ADMIN);
    Participant member = Participant.of(memberId, Role.MEMBER);
    ManageIntent intent = new ManageIntent(initiator, member);
    InviteDecision decision = new InviteDecision(intent);
    InternalInteractionContext context = new InternalInteractionContext(community);

    AtomicBoolean called = new AtomicBoolean(false);
    InteractionHandler<ManageIntent> handler = new RecordingHandler(called);

    Interaction interaction = MemberManagementDomainService.of()
        .execute(context, decision, handler);

    assertThat(called.get()).isTrue();
    assertThat(interaction.isPendingApproval()).isTrue();
  }

  @Test
  @DisplayName("관리 결정의 initiator가 참여자가 아니면 예외가 발생한다")
  void executeThrowsWhenInitiatorNotParticipant() {
    UUID adminId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    Community community = Cooperative.create(
        "org",
        "desc",
        adminId,
        Role.ADMIN,
        Role.MEMBER
    );

    Participant outsider = Participant.of(outsiderId, Role.ADMIN);
    Participant member = Participant.of(UUID.randomUUID(), Role.MEMBER);
    InviteDecision decision = new InviteDecision(new ManageIntent(outsider, member));
    InternalInteractionContext context = new InternalInteractionContext(community);

    assertThatThrownBy(() -> MemberManagementDomainService.of()
        .execute(context, decision, new RecordingHandler(new AtomicBoolean())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  static final class RecordingHandler implements InteractionHandler<ManageIntent> {
    private final AtomicBoolean called;

    RecordingHandler(AtomicBoolean called) {
      this.called = called;
    }

    @Override
    public boolean supports(Decision<?> decision) {
      return decision instanceof InviteDecision;
    }

    @Override
    public void handle(InteractionContext context, Decision<? extends ManageIntent> decision) {
      called.set(true);
    }
  }
}
