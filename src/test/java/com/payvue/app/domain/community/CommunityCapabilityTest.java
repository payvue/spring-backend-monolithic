package com.payvue.app.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.community.policy.CommunityPolicy;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityCapabilityTest {

  @Test
  @DisplayName("개인 커뮤니티는 멤버 추가/제거/역할 변경이 불가하고 상태가 유지된다")
  void personalCommunityRejectsMemberOperations() {
    Personal personal = Personal.create("name", "desc", UUID.randomUUID(), Role.ADMIN);
    CommunityPolicy allow = community -> true;
    int initialCount = personal.getParticipantCount();
    UUID adminId = personal.getParticipantIds().iterator().next();

    assertThatThrownBy(() -> personal.addMember(UUID.randomUUID(), allow))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> personal.removeMember(adminId, allow))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> personal.changeParticipantRole(adminId, Role.MEMBER, allow))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThat(personal.getParticipantCount()).isEqualTo(initialCount);
    assertThat(personal.getParticipantIds()).containsExactly(adminId);
    assertThat(personal.isAdmin(adminId)).isTrue();
  }

  @Test
  @DisplayName("조직 커뮤니티는 정책에 따라 멤버 추가와 역할 변경을 수행한다")
  void organizationCommunityAddsAndChangesRole() {
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    CommunityPolicy allow = community -> true;
    UUID memberId = UUID.randomUUID();

    organization.addMember(memberId, allow);

    assertThat(organization.isParticipant(memberId)).isTrue();
    assertThat(organization.getParticipantRole(memberId).getName()).isEqualTo("MEMBER");

    organization.changeParticipantRole(memberId, Role.GUEST, allow);

    assertThat(organization.getParticipantRole(memberId).getName()).isEqualTo("GUEST");
    assertThat(organization.getParticipantCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("조직 커뮤니티는 정책이 실패하면 상태가 변하지 않는다")
  void organizationCommunityDoesNotChangeWhenPolicyFails() {
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    CommunityPolicy deny = community -> false;
    int initialCount = organization.getParticipantCount();
    UUID memberId = UUID.randomUUID();

    organization.addMember(memberId, deny);
    organization.changeParticipantRole(memberId, Role.GUEST, deny);

    assertThat(organization.getParticipantCount()).isEqualTo(initialCount);
    assertThat(organization.isParticipant(memberId)).isFalse();
  }

  @Test
  @DisplayName("조직 커뮤니티는 결정권자 제거 시 예외가 발생한다")
  void organizationCommunityCannotRemoveAdmin() {
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    CommunityPolicy allow = community -> true;
    UUID adminId = organization.getParticipantIds().iterator().next();

    assertThatThrownBy(() -> organization.removeMember(adminId, allow))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("조직 커뮤니티는 일반 멤버 제거 시 참여자 수가 감소한다")
  void organizationCommunityRemovesMember() {
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    CommunityPolicy allow = community -> true;
    UUID memberId = UUID.randomUUID();
    organization.addMember(memberId, allow);
    int countAfterAdd = organization.getParticipantCount();

    organization.removeMember(memberId, allow);

    assertThat(organization.getParticipantCount()).isEqualTo(countAfterAdd - 1);
    assertThat(organization.isParticipant(memberId)).isFalse();
  }
}
