package com.payvue.app.infrastructure.persistence.assetholding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.asset.category.Coupon;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.Ownable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.UserId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssetItemMapperTest {

  private final AssetCharacteristicMapper capabilityMapper = new AssetCharacteristicMapper(new ObjectMapper());
  private final AssetItemMapper mapper = new AssetItemMapper(capabilityMapper);

  @Test
  @DisplayName("자산 도메인을 엔티티로 변환한다")
  void mapsDomainToEntity() {
    CommunityId communityId = CommunityId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    UserId contributorId = UserId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    AssetItem item = Coupon.create(communityId, contributorId, "Coupon")
        .with(Consumable.multipleUse(4))
        .with(Ownable.personal());

    AssetItemTBL entity = mapper.toEntity(item);

    assertThat(entity.getOwnerCommunityId()).isEqualTo(communityId.getValue());
    assertThat(entity.getOwnerUserId()).isEqualTo(contributorId.getValue());
    assertThat(entity.getCategory()).isEqualTo("COUPON");
    assertThat(entity.getAmount()).isEqualTo(4);
    assertThat(entity.getCapability()).isNotNull();
  }

  @Test
  @DisplayName("자산 엔티티를 도메인으로 복원한다")
  void mapsEntityToDomain() {
    UUID ownerUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    UUID ownerCommunityId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    AssetItemTBL entity = AssetItemTBL.create(
        2,
        "Coupon",
        "COUPON",
        "ACTIVE",
        ownerUserId,
        ownerCommunityId
    );
    AssetItemCapabilityTBL capability = new AssetCharacteristicMapper(new ObjectMapper())
        .toEntity(entity, java.util.Set.of(Consumable.singleUse(2), Ownable.shared()));
    entity.setCapability(capability);

    AssetItem item = mapper.toDomain(entity);

    assertThat(item.getCommunityId().getValue()).isEqualTo(ownerCommunityId);
    assertThat(item.getName()).isEqualTo("Coupon");
    assertThat(item.getCategory().getValue()).isEqualTo("COUPON");
    assertThat(item.getCapabilities()).isNotEmpty();
  }

  @Test
  @DisplayName("capability가 없으면 빈 컬렉션으로 복원된다")
  void mapsEntityWithoutCapabilities() {
    UUID ownerCommunityId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    AssetItemTBL entity = AssetItemTBL.create(
        null,
        "Gift",
        "GIFT",
        "ACTIVE",
        null,
        ownerCommunityId
    );

    AssetItem item = mapper.toDomain(entity);

    assertThat(item.getCommunityId().getValue()).isEqualTo(ownerCommunityId);
    assertThat(item.getCapabilities()).isEmpty();
  }

  @Test
  @DisplayName("capability가 없으면 amount가 null로 저장된다")
  void mapsDomainWithoutCapabilitiesToNullAmount() {
    CommunityId communityId = CommunityId.of(UUID.fromString("66666666-6666-6666-6666-666666666666"));
    AssetItem item = Coupon.create(communityId, null, "Coupon");

    AssetItemTBL entity = mapper.toEntity(item);

    assertThat(entity.getAmount()).isNull();
  }
}
