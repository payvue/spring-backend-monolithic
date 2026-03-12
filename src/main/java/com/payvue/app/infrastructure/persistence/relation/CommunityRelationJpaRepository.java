package com.payvue.app.infrastructure.persistence.relation;

import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CommunityRelationJpaRepository extends JpaRepository<CommunityRelationTBL, UUID> {
  Optional<CommunityRelationTBL> findByFromCommunityIdAndToCommunityId(UUID fromCommunityId, UUID toCommunityId);

  boolean existsByFromCommunityIdAndToCommunityId(UUID fromCommunityId, UUID toCommunityId);

  Page<CommunityRelationTBL> findByFromCommunityId(UUID fromCommunityId, Pageable pageable);

  List<CommunityRelationTBL> findByFromCommunityIdAndRelationType(UUID fromCommunityId, RelationType type);

  List<CommunityRelationTBL> findByFromCommunityIdOrToCommunityId(UUID fromCommunityId, UUID toCommunityId);

  @Query("""
      select r
        from CommunityRelationTBL r
       where (r.fromCommunityId = :communityId or r.toCommunityId = :communityId)
         and r.status = :status
      """)
  List<CommunityRelationTBL> findByCommunityIdAndStatus(
      @Param("communityId") UUID communityId,
      @Param("status") ConnectionStatus status
  );

  @Query("""
      select r
        from CommunityRelationTBL r
       where (r.fromCommunityId = :fromCommunityId or r.toCommunityId = :toCommunityId)
         and r.status = :status
      """)
  List<CommunityRelationTBL> findByFromCommunityIdOrToCommunityIdAndStatus(
      @Param("fromCommunityId") UUID fromCommunityId,
      @Param("toCommunityId") UUID toCommunityId,
      @Param("status") ConnectionStatus status
  );
}
