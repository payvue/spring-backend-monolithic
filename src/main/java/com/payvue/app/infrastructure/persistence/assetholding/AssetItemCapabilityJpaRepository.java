package com.payvue.app.infrastructure.persistence.assetholding;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssetItemCapabilityJpaRepository extends JpaRepository<AssetItemCapabilityTBL, UUID> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update AssetItemCapabilityTBL c
         set c.configJson = :#{#entity.configJson}
       where c.assetItem.id = :#{#entity.assetItem.id}
      """)
  int updateConfigByAssetItemId(@Param("entity") AssetItemCapabilityTBL capability);
}
