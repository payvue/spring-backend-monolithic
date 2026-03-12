package com.payvue.app.infrastructure.persistence.assetholding;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssetItemJpaRepository extends JpaRepository<AssetItemTBL, UUID> {
  List<AssetItemTBL> findByOwnerCommunityId(UUID ownerCommunityId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update AssetItemTBL a
         set a.name = :#{#entity.name},
             a.amount = :#{#entity.amount},
             a.category = :#{#entity.category},
             a.status = :#{#entity.status},
             a.ownerUserId = :#{#entity.ownerUserId},
             a.ownerCommunityId = :#{#entity.ownerCommunityId},
             a.updatedAt = :#{#entity.updatedAt}
       where a.id = :#{#entity.id}
      """)
  int updateAssetItem(@Param("entity") AssetItemTBL entity);
}
