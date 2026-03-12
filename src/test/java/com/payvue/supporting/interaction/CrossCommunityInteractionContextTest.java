package com.payvue.supporting.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.relation.CommunityRelation;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrossCommunityInteractionContextTest {

  @Test
  @DisplayName("활성 관계에서 관리 권한이 있으면 교차 컨텍스트 검증을 통과한다")
  void validatePassesWhenRelationActive() {
    UUID adminId = UUID.randomUUID();
    Community source = Personal.create("me", "desc", adminId, Role.ADMIN);
    Community target = Personal.create("other", "desc", UUID.randomUUID(), Role.ADMIN);
    CommunityRelation relation = CommunityRelation.associateWith(source, target);

    Participant initiator = Participant.of(adminId, Role.ADMIN);
    TransferAssetIntent intent = new TransferAssetIntent(initiator, null, null, null);
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(source, target, relation);

    assertThat(context.validate(decision)).isTrue();
  }

  @Test
  @DisplayName("활성 관계에서 관리 권한이 없으면 교차 컨텍스트 검증을 실패한다")
  void validateFailsWhenRelationActive() {
    UUID adminId = UUID.randomUUID();
    Community source = Personal.create("me", "desc", adminId, Role.ADMIN);
    Community target = Personal.create("other", "desc", UUID.randomUUID(), Role.ADMIN);
    CommunityRelation relation = CommunityRelation.associateWith(source, target);

    Participant initiator = Participant.of(adminId, Role.MEMBER);
    TransferAssetIntent intent = new TransferAssetIntent(initiator, null, null, null);
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(source, target, relation);

    assertThat(context.validate(decision)).isFalse();
  }

  @Test
  @DisplayName("비활성 관계에서는 교차 컨텍스트 검증에 실패한다")
  void validateFailsWhenRelationInactive() {
    UUID adminId = UUID.randomUUID();
    Community source = Personal.create("me", "desc", adminId, Role.ADMIN);
    Community target = Personal.create("other", "desc", UUID.randomUUID(), Role.ADMIN);
    CommunityRelation relation = CommunityRelation.associateWith(source, target);
    relation.suspend();

    Participant initiator = Participant.of(adminId, Role.ADMIN);
    TransferAssetIntent intent = new TransferAssetIntent(initiator, null, null, null);
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(source, target, relation);

    assertThat(context.validate(decision)).isFalse();
  }

  @Test
  @DisplayName("initiator가 소스 커뮤니티 참여자가 아니면 검증에 실패한다")
  void validateFailsWhenInitiatorNotParticipant() {
    UUID adminId = UUID.randomUUID();
    Community source = Personal.create("me", "desc", adminId, Role.ADMIN);
    Community target = Personal.create("other", "desc", UUID.randomUUID(), Role.ADMIN);
    CommunityRelation relation = CommunityRelation.associateWith(source, target);

    Participant initiator = Participant.of(UUID.randomUUID(), Role.ADMIN);
    TransferAssetIntent intent = new TransferAssetIntent(initiator, null, null, null);
    ApproveDecision<TransferAssetIntent> decision = new ApproveDecision<>(intent);

    CrossCommunityInteractionContext context = new CrossCommunityInteractionContext(source, target, relation);

    assertThat(context.validate(decision)).isFalse();
  }
}
