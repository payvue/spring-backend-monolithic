package com.payvue.app.infrastructure.persistence.assetholding;

import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Coupon;
import com.payvue.app.domain.asset.category.GameItem;
import com.payvue.app.domain.asset.category.GiftCard;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.app.domain.asset.category.MusicItem;
import com.payvue.app.domain.asset.category.ShoppingItem;
import com.payvue.core.community.CommunityId;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

@Component
public class AssetItemMapper {

  private final AssetCharacteristicMapper capabilityMapper;

  public AssetItemMapper(AssetCharacteristicMapper capabilityMapper) {
    this.capabilityMapper = capabilityMapper;
  }

  public AssetItem toDomain(AssetItemTBL entity) {
    if (entity == null) {
      return null;
    }
    AssetItem item = createAssetItem(
        entity.getCategory(),
        entity.getName(),
        CommunityId.of(entity.getOwnerCommunityId())
    );

    AssetStatus status = AssetStatus.valueOf(entity.getStatus().toUpperCase());
    AssetItem mapped = withIdentity(item, entity.getId(), status);

    if (entity.getCapability() != null) {
      for (AssetCharacteristic capability : capabilityMapper.toDomain(entity.getCapability())) {
        mapped.with(capability);
      }
    }

    return mapped;
  }

  public AssetItemTBL toEntity(AssetItem domain) {
    if (domain == null) {
      return null;
    }

    Integer amount = null;
    for (AssetCharacteristic capability : domain.getCapabilities()) {
      if (capability instanceof Consumable consumable) {
        amount = consumable.quantity();
      }
    }

    AssetItemTBL entity = AssetItemTBL.create(
        amount,
        domain.getName(),
        domain.getCategory().getValue(),
        domain.getStatus().name(),
        domain.getContributorId() != null ? domain.getContributorId().getValue() : null,
        domain.getCommunityId().getValue()
    );

    setEntityId(entity, domain.getId().getValue());
    Set<AssetCharacteristic> capabilities = domain.getCapabilities();
    if (capabilities != null && !capabilities.isEmpty()) {
      entity.setCapability(capabilityMapper.toEntity(entity, capabilities));
    }
    return entity;
  }

  private AssetItem createAssetItem(String category, String name, CommunityId communityId) {
    String normalized = category == null ? "" : category.toUpperCase();
    return switch (normalized) {
      case "CASH" -> Cash.create(communityId, null, name);
      case "MILEAGE" -> Mileage.create(communityId, null, name);
      case "GIFT" -> GiftCard.create(communityId, null, name);
      case "GAME" -> GameItem.create(communityId, null, name);
      case "MUSIC" -> MusicItem.create(communityId, null, name);
      case "SHOPPING" -> ShoppingItem.create(communityId, null, name);
      case "COUPON" -> Coupon.create(communityId, null, name);
      default -> throw new IllegalArgumentException("Unsupported asset category: " + category);
    };
  }

  private AssetItem withIdentity(AssetItem item, UUID id, AssetStatus status) {
    try {
      Field idField = AssetItem.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(item, AssetItemId.of(id));

      Field statusField = AssetItem.class.getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(item, status);

      return item;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set asset identity", e);
    }
  }

  private void setEntityId(AssetItemTBL entity, UUID id) {
    try {
      Field idField = AssetItemTBL.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set entity id", e);
    }
  }
}
