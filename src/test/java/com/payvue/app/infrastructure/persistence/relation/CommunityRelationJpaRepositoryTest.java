package com.payvue.app.infrastructure.persistence.relation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class CommunityRelationJpaRepositoryTest {

  @Autowired
  private CommunityRelationJpaRepository repository;

  @Test
  @DisplayName("관계 ID로 조회한다")
  void findsByFromAndTo() {
    UUID fromId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID toId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        fromId,
        toId,
        RelationType.NEIGHBOR,
        ConnectionStatus.PENDING
    );
    repository.save(entity);

    assertThat(repository.findByFromCommunityIdAndToCommunityId(fromId, toId)).isPresent();
    assertThat(repository.existsByFromCommunityIdAndToCommunityId(fromId, toId)).isTrue();
  }

  @Test
  @DisplayName("출발 커뮤니티로 페이지 조회한다")
  void findsByFromCommunityId() {
    UUID fromId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        fromId,
        UUID.fromString("66666666-6666-6666-6666-666666666666"),
        RelationType.PROVIDER,
        ConnectionStatus.ACTIVE
    );
    repository.save(entity);

    List<CommunityRelationTBL> results = repository.findByFromCommunityId(fromId, PageRequest.of(0, 10)).getContent();

    assertThat(results).hasSize(1);
  }

  @Test
  @DisplayName("관계 타입으로 조회한다")
  void findsByRelationType() {
    UUID fromId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("88888888-8888-8888-8888-888888888888"),
        fromId,
        UUID.fromString("99999999-9999-9999-9999-999999999999"),
        RelationType.SUBSCRIBER,
        ConnectionStatus.ACTIVE
    );
    repository.save(entity);

    List<CommunityRelationTBL> results = repository.findByFromCommunityIdAndRelationType(fromId, RelationType.SUBSCRIBER);

    assertThat(results).hasSize(1);
  }

  @Test
  @DisplayName("관계가 없으면 비어 있는 결과를 반환한다")
  void returnsEmptyWhenNoRelation() {
    UUID fromId = UUID.fromString("12121212-1212-1212-1212-121212121212");

    List<CommunityRelationTBL> results = repository.findByFromCommunityIdAndRelationType(fromId, RelationType.SUBSCRIBER);

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("상태 조건으로 조회한다")
  void findsByStatus() {
    UUID fromId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    UUID toId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
        fromId,
        toId,
        RelationType.FRENDSHIP,
        ConnectionStatus.PENDING
    );
    repository.save(entity);

    List<CommunityRelationTBL> results = repository.findByFromCommunityIdOrToCommunityIdAndStatus(
        fromId,
        toId,
        ConnectionStatus.PENDING
    );

    assertThat(results).hasSize(1);
  }
}
