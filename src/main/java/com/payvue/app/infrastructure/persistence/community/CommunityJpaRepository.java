package com.payvue.app.infrastructure.persistence.community;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityJpaRepository extends JpaRepository<CommunityTBL, UUID> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update CommunityTBL c
         set c.name = :#{#entity.name},
             c.description = :#{#entity.description},
             c.updatedAt = :#{#entity.updatedAt}
       where c.id = :#{#entity.id}
      """)
  int updateCommunity(@Param("entity") CommunityTBL entity);

  @Query("""
      select distinct c
        from CommunityTBL c
        left join fetch c.participants p
       where c.id in :ids
      """)
  List<CommunityTBL> findAllByIdInWithParticipants(@Param("ids") Set<UUID> ids);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      delete from ParticipantTBL p
       where p.community.id = :communityId
      """)
  int deleteParticipantsByCommunityId(@Param("communityId") UUID communityId);

  boolean existsByName(String name);
}
