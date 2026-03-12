package com.payvue.app.infrastructure.persistence.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.common.status.ExecutionState;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class TransactionJpaRepositoryTest {

  @Autowired
  private TransactionJpaRepository repository;

  @Test
  @DisplayName("actorId와 communityId로 조회한다")
  void findsByActorIdAndCommunityId() {
    TransactionEntity entity = new TransactionEntity(
        "tx-1",
        TransactionType.TRANSFER,
        "CASH",
        "community-1",
        "from-community-1",
        "to-community-1",
        "item-1",
        "actor-1",
        1000L,
        "desc",
        "category",
        TransactionMeta.fromRawMap(Map.of("CLIENT_REQUEST_ID", "value")),
        ExecutionState.PENDING_APPROVAL,
        Instant.parse("2024-01-01T00:00:00Z")
    );
    repository.save(entity);

    List<TransactionEntity> byActor = repository.findByActorId("actor-1");
    List<TransactionEntity> byCommunity = repository.findByCommunityId("community-1");

    assertThat(byActor).hasSize(1);
    assertThat(byCommunity).hasSize(1);
  }
}
