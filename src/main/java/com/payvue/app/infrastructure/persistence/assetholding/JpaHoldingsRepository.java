package com.payvue.app.infrastructure.persistence.assetholding;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.HoldingSpecification;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsId;
import com.payvue.core.hold.HoldingsRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaHoldingsRepository implements HoldingsRepository {

  private final HoldingsJpaRepository holdingsJpaRepository;
  private final AssetItemJpaRepository assetItemJpaRepository;
  private final AssetItemCapabilityJpaRepository assetItemCapabilityJpaRepository;
  private final AssetItemMapper mapper;

  @Override
  public boolean existsById(HoldingsId id) {
    if (id == null) {
      return false;
    }
    return holdingsJpaRepository.existsById(id.getValue());
  }

  @Override
  public Holdings findByCommunity(Community community) {
    if (community == null) {
      throw new IllegalArgumentException("커뮤니티를 조회할 수 없습니다.");
    }
    HoldingsTBL entity = holdingsJpaRepository.findByCommunityId(community.getId().getValue())
        .orElse(null);
    List<AssetItem> assets = assetItemJpaRepository
        .findByOwnerCommunityId(community.getId().getValue())
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
    HoldingsId id = entity == null ? HoldingsId.generate() : HoldingsId.of(entity.getId());
    return Holdings.reconstitute(id, community.getId(), assets);
  }

  @Override
  public Set<Holdings> findByCommunities(Set<Community> communities) {
    if (communities == null || communities.isEmpty()) {
      return Set.of();
    }
    Set<Holdings> results = new HashSet<>();
    for (Community community : communities) {
      if (community == null) {
        continue;
      }
      results.add(findByCommunity(community));
    }
    return results;
  }

  @Override
  public Set<AssetItem> findByCommunityBySepcification(
      Community community,
      HoldingSpecification specification
  ) {
    if (community == null || specification == null) {
      return Set.of();
    }
    return findByCommunity(community).ownedAssetsConformingTo(specification);
  }

  @Override
  public Set<AssetItem> findByCommunitiesBySepcfication(
      Set<Community> communities,
      HoldingSpecification specification
  ) {
    if (communities == null || communities.isEmpty() || specification == null) {
      return Set.of();
    }
    Set<AssetItem> assets = new HashSet<>();
    for (Community community : communities) {
      if (community == null) {
        continue;
      }
      assets.addAll(findByCommunityBySepcification(community, specification));
    }
    return assets;
  }

  @Override
  @Transactional
  public void save(Holdings holdings) {
    if (holdings == null) {
      return;
    }
    CommunityId communityId = holdings.getCommunityId();
    if (communityId == null) {
      throw new IllegalArgumentException("communityId is required");
    }
    HoldingsId holdingsId = holdings.getId();
    HoldingsTBL entity = holdingsJpaRepository.findById(holdingsId.getValue())
        .orElseGet(() -> HoldingsTBL.create(holdingsId.getValue(), communityId.getValue()));
    holdingsJpaRepository.save(entity);

    reconcileAssets(communityId.getValue(), holdings.getAssets());
  }

  private void reconcileAssets(UUID communityId, List<AssetItem> assets) {
    List<AssetItemTBL> existing = assetItemJpaRepository.findByOwnerCommunityId(communityId);
    Set<UUID> currentIds = assets.stream()
        .map(AssetItem::getId)
        .filter(Objects::nonNull)
        .map(AssetItemId::getValue)
        .collect(Collectors.toSet());

    List<UUID> toDelete = existing.stream()
        .map(AssetItemTBL::getId)
        .filter(id -> !currentIds.contains(id))
        .collect(Collectors.toList());
    if (!toDelete.isEmpty()) {
      assetItemJpaRepository.deleteAllById(toDelete);
    }

    for (AssetItem asset : assets) {
      upsertAsset(asset);
    }
  }

  private void upsertAsset(AssetItem item) {
    if (item == null) {
      return;
    }
    AssetItemTBL entity = mapper.toEntity(item);
    if (entity.getId() != null && assetItemJpaRepository.existsById(entity.getId())) {
      assetItemJpaRepository.updateAssetItem(entity);
      upsertCapability(entity);
      return;
    }
    AssetItemTBL saved = assetItemJpaRepository.save(entity);
    upsertCapability(saved);
  }

  private void upsertCapability(AssetItemTBL entity) {
    if (entity.getCapability() == null) {
      return;
    }
    int updated = assetItemCapabilityJpaRepository.updateConfigByAssetItemId(
        entity.getCapability()
    );
    if (updated == 0) {
      assetItemCapabilityJpaRepository.save(entity.getCapability());
    }
  }
}
