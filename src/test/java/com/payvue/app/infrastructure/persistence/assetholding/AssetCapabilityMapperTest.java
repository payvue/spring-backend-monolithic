package com.payvue.app.infrastructure.persistence.assetholding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.characteristic.AssetCharacteristic;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.characteristic.Ownable;
import com.payvue.shared.Money;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssetCharacteristicMapperTest {

  private final AssetCharacteristicMapper mapper = new AssetCharacteristicMapper(new ObjectMapper());

  @Test
  @DisplayName("capability 구성을 엔티티 JSON으로 직렬화한다")
  void mapsCapabilitiesToEntity() {
    AssetItemTBL item = AssetItemTBL.create(
        3,
        "coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        UUID.fromString("22222222-2222-2222-2222-222222222222")
    );
    Set<AssetCharacteristic> capabilities = Set.of(
        HasBalance.of(Money.of(1000L, "KRW")),
        Consumable.multipleUse(3),
        Ownable.personal()
    );

    AssetItemCapabilityTBL entity = mapper.toEntity(item, capabilities);

    assertThat(entity.getConfigJson()).contains("balance");
    assertThat(entity.getConfigJson()).contains("quantity");
    assertThat(entity.getConfigJson()).contains("type");
  }

  @Test
  @DisplayName("capability 엔티티 JSON을 도메인으로 복원한다")
  void mapsEntityToCapabilities() {
    AssetItemTBL item = AssetItemTBL.create(
        5,
        "coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        UUID.fromString("44444444-4444-4444-4444-444444444444")
    );
    Set<AssetCharacteristic> capabilities = Set.of(
        Consumable.singleUse(5),
        Ownable.shared()
    );
    AssetItemCapabilityTBL entity = mapper.toEntity(item, capabilities);

    Set<AssetCharacteristic> restored = mapper.toDomain(entity);

    assertThat(restored).hasSize(2);
    assertThat(restored.stream().anyMatch(cap -> cap instanceof Consumable)).isTrue();
    assertThat(restored.stream().anyMatch(cap -> cap instanceof Ownable)).isTrue();
  }

  @Test
  @DisplayName("capability가 비어 있으면 엔티티로 변환하지 않는다")
  void mapsEmptyCapabilitiesToNull() {
    AssetItemTBL item = AssetItemTBL.create(
        1,
        "coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        UUID.fromString("66666666-6666-6666-6666-666666666666")
    );

    AssetItemCapabilityTBL entity = mapper.toEntity(item, Set.of());

    assertThat(entity).isNull();
  }

  @Test
  @DisplayName("capability 엔티티가 null이면 빈 Set을 반환한다")
  void mapsNullEntityToEmptySet() {
    assertThat(mapper.toDomain(null)).isEmpty();
  }
}
