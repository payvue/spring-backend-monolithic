package com.payvue.app.domain.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.core.community.policy.CommunityPolicy;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityDomainTest {

  @Test
  @DisplayName("개인 커뮤니티는 최대 인원과 타입을 보장한다")
  void personalCommunityHasMaxMembersAndType() {
    Personal personal = Personal.create(
        "name",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN
    );

    assertThat(personal.getCommunityType()).isEqualTo(CommunityCategory.PERSONAL.name());
    assertThat(personal.getMaxMembers()).isEqualTo(1);
    assertThat(personal.getParticipantCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("조직 커뮤니티는 멤버 추가/역할 변경/소유권 이전이 가능하다")
  void organizationCommunitySupportsMemberOperations() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    UUID ownerId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        ownerId,
        roleCatalog.admin(),
        roleCatalog.member()
    );
    CommunityPolicy allow = community -> true;

    organization.addMember(memberId, allow);
    assertThat(organization.getParticipantCount()).isEqualTo(2);
    assertThat(organization.getParticipantRole(memberId).getName()).isEqualTo("MEMBER");

    organization.changeParticipantRole(memberId, roleCatalog.guest(), allow);
    assertThat(organization.getParticipantRole(memberId).getName()).isEqualTo("GUEST");

    organization.transferOwnership(ownerId, memberId, allow);
    assertThat(organization.getParticipantRole(memberId).getName()).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("조직 커뮤니티는 결정권자를 제거할 수 없다")
  void organizationCommunityCannotRemoveAdmin() {
    Cooperative organization = Cooperative.create(
        "org",
        "desc",
        UUID.randomUUID(),
        Role.ADMIN,
        Role.MEMBER
    );
    CommunityPolicy allow = community -> true;

    assertThatThrownBy(() -> organization.removeMember(organization.getParticipantIds().iterator().next(), allow))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("RoleCatalog는 등록된 역할을 조회하고 기본값은 GUEST이다")
  void roleCatalogResolvesRoles() {
    RoleCatalog catalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();

    assertThat(catalog.resolve("ADMIN").getName()).isEqualTo("ADMIN");
    assertThat(catalog.resolve("unknown").getName()).isEqualTo("GUEST");
  }
}
