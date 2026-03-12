package com.payvue.app.infrastructure.persistence.assetholding;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HoldingsJpaRepository extends JpaRepository<HoldingsTBL, UUID> {
  Optional<HoldingsTBL> findByCommunityId(UUID communityId);
}
