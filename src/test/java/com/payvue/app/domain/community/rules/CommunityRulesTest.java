package com.payvue.app.domain.community.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.core.community.policy.CommunityPolicy;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityRulesTest {

  @Test
  @DisplayName("IsParticipantRule은 참여자 여부를 확인한다")
  void isParticipantRuleChecksMembership() {
    UUID ownerId = UUID.randomUUID();
    Cooperative organization = Cooperative.create("org", "desc", ownerId, Role.ADMIN, Role.MEMBER);
    IsParticipantRule rule = new IsParticipantRule(ownerId);

    assertThat(rule.evaluate(organization)).isTrue();
  }

  @Test
  @DisplayName("HasDecisionAuthorityRule은 관리자 여부를 확인한다")
  void hasDecisionAuthorityRuleChecksAdmin() {
    UUID ownerId = UUID.randomUUID();
    Cooperative organization = Cooperative.create("org", "desc", ownerId, Role.ADMIN, Role.MEMBER);
    HasDecisionAuthorityRule rule = new HasDecisionAuthorityRule(ownerId);

    assertThat(rule.evaluate(organization)).isTrue();
  }

  @Test
  @DisplayName("MoreThanOneParticipantRule은 참여자 수가 1보다 큰지 확인한다")
  void moreThanOneParticipantRuleChecksCount() {
    UUID ownerId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    Cooperative organization = Cooperative.create("org", "desc", ownerId, Role.ADMIN, Role.MEMBER);
    CommunityPolicy allow = community -> true;
    organization.addMember(memberId, allow);

    MoreThanOneParticipantRule rule = new MoreThanOneParticipantRule();
    assertThat(rule.evaluate(organization)).isTrue();
  }

  @Test
  @DisplayName("MaxMembersRule은 최대 인원 제한을 확인한다")
  void maxMembersRuleChecksLimit() {
    UUID ownerId = UUID.randomUUID();
    Cooperative organization = Cooperative.create("org", "desc", ownerId, Role.ADMIN, Role.MEMBER);

    MaxMembersRule rule = new MaxMembersRule(2);
    assertThat(rule.evaluate(organization)).isTrue();
  }
}
