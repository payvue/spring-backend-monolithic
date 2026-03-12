package com.payvue.app.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.core.community.Community;
import com.payvue.supporting.interaction.ChangeRoleDecision;
import com.payvue.supporting.interaction.Decision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionContextFactory;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.InteractionFlowRegistry;
import com.payvue.supporting.interaction.InviteDecision;
import com.payvue.supporting.interaction.ManageIntent;
import com.payvue.supporting.user.command.User;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberManagerImplTest {

  @Test
  @DisplayName("초대 요청은 관리자/멤버 역할로 구성된 인텐트를 전달한다")
  void inviteUsesAdminAndMemberRoles() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .build();
    StubInteractionExecutor executor = new StubInteractionExecutor();
    MemberManagerImpl manager = new MemberManagerImpl(executor, roleCatalog);
    Community community = Personal.create("name", "desc", UUID.randomUUID(), roleCatalog.admin());
    User initiator = newUser("init@pay.com");
    User invitee = newUser("invitee@pay.com");

    manager.invite(community, initiator, invitee);

    assertThat(executor.lastDecision).isInstanceOf(InviteDecision.class);
    ManageIntent intent = ((InviteDecision) executor.lastDecision).getIntent();
    assertThat(intent.initiator().getRole().getName()).isEqualTo("ADMIN");
    assertThat(intent.participant().getRole().getName()).isEqualTo("MEMBER");
  }

  @Test
  @DisplayName("역할 변경은 ChangeRoleDecision과 신규 역할을 전달한다")
  void changeRoleUsesDecisionWithNewRole() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    StubInteractionExecutor executor = new StubInteractionExecutor();
    MemberManagerImpl manager = new MemberManagerImpl(executor, roleCatalog);
    Community community = Personal.create("name", "desc", UUID.randomUUID(), roleCatalog.admin());
    User initiator = newUser("init@pay.com");
    User target = newUser("target@pay.com");

    manager.changeRole(community, initiator, target, roleCatalog.guest());

    assertThat(executor.lastDecision).isInstanceOf(ChangeRoleDecision.class);
    ChangeRoleDecision decision = (ChangeRoleDecision) executor.lastDecision;
    assertThat(decision.getNewRole().getName()).isEqualTo("GUEST");
    ManageIntent intent = decision.getIntent();
    assertThat(intent.initiator().getRole().getName()).isEqualTo("ADMIN");
    assertThat(intent.participant().getRole().getName()).isEqualTo("MEMBER");
  }

  @Test
  @DisplayName("사용자 정보가 없으면 초대 시 예외가 발생한다")
  void inviteThrowsWhenUserMissing() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .build();
    StubInteractionExecutor executor = new StubInteractionExecutor();
    MemberManagerImpl manager = new MemberManagerImpl(executor, roleCatalog);
    Community community = Personal.create("name", "desc", UUID.randomUUID(), roleCatalog.admin());

    assertThatThrownBy(() -> manager.invite(community, null, newUser("invitee@pay.com")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private User newUser(String email) {
    return User.create(
        "login-" + email,
        email,
        "010",
        "nick",
        "real",
        LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345"
    );
  }

  static final class StubInteractionExecutor extends InteractionExecutor {
    Decision<?> lastDecision;
    Community lastCommunity;

    StubInteractionExecutor() {
      super(new InteractionContextFactory(), new InteractionFlowRegistry());
    }

    @Override
    public Interaction executeInternal(Decision<?> decision, Community community) {
      this.lastDecision = decision;
      this.lastCommunity = community;
      return Interaction.pending(decision);
    }
  }
}
