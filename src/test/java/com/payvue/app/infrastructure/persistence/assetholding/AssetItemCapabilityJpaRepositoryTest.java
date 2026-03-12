package com.payvue.app.infrastructure.persistence.assetholding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.Ownable;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AssetItemCapabilityJpaRepositoryTest {

  @Autowired
  private AssetItemCapabilityJpaRepository repository;

  @Autowired
  private AssetItemJpaRepository assetItemJpaRepository;

  @Test
  @DisplayName("자산 capability 설정을 업데이트한다")
  void updatesCapabilityConfig() {
    AssetItemTBL asset = AssetItemTBL.create(
        2,
        "Coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("11111111-2222-3333-4444-555555555555"),
        UUID.fromString("66666666-7777-8888-9999-000000000000")
    );
    assetItemJpaRepository.saveAndFlush(asset);

    AssetCharacteristicMapper mapper = new AssetCharacteristicMapper(new ObjectMapper());
    AssetItemCapabilityTBL capability = mapper.toEntity(asset, Set.of(Consumable.singleUse(2), Ownable.personal()));
    repository.save(capability);

    AssetItemCapabilityTBL updated = mapper.toEntity(asset, Set.of(Consumable.multipleUse(5), Ownable.shared()));

    int count = repository.updateConfigByAssetItemId(updated);

    assertThat(count).isEqualTo(1);
    AssetItemCapabilityTBL reloaded = repository.findById(capability.getId()).orElseThrow();
    assertThat(reloaded.getConfigJson()).contains("quantity");
  }

  @Test
  @DisplayName("존재하지 않는 자산의 capability 업데이트는 0을 반환한다")
  void updatesCapabilityReturnsZeroWhenMissing() {
    AssetItemTBL asset = AssetItemTBL.create(
        1,
        "Coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
        UUID.fromString("ffffffff-1111-2222-3333-444444444444")
    );
    assetItemJpaRepository.saveAndFlush(asset);

    AssetCharacteristicMapper mapper = new AssetCharacteristicMapper(new ObjectMapper());
    AssetItemCapabilityTBL capability = mapper.toEntity(asset, Set.of(Consumable.singleUse(1), Ownable.personal()));
    repository.save(capability);

    AssetItemTBL other = AssetItemTBL.create(
        1,
        "Other",
        "COUPON",
        "ACTIVE",
        UUID.fromString("11111111-2222-3333-4444-555555555555"),
        UUID.fromString("66666666-7777-8888-9999-000000000000")
    );
    assetItemJpaRepository.saveAndFlush(other);
    AssetItemCapabilityTBL updated = mapper.toEntity(other, Set.of(Consumable.multipleUse(3), Ownable.shared()));

    int count = repository.updateConfigByAssetItemId(updated);

    assertThat(count).isEqualTo(0);
  }
}
