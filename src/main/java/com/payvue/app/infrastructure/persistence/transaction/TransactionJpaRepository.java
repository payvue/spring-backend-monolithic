package com.payvue.app.infrastructure.persistence.transaction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {

  List<TransactionEntity> findByActorId(String actorId);

  List<TransactionEntity> findByCommunityId(String communityId);

  List<TransactionEntity> findByFromCommunityIdOrToCommunityId(String fromCommunityId, String toCommunityId);
}
