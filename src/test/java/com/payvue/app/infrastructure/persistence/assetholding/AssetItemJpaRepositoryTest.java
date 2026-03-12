package com.payvue.app.infrastructure.persistence.assetholding;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AssetItemJpaRepositoryTest {

  @Autowired
  private AssetItemJpaRepository repository;

  @Test
  @DisplayName("자산 엔티티를 업데이트한다")
  void updatesAssetItem() {
    UUID communityId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    AssetItemTBL entity = AssetItemTBL.create(
        1,
        "Coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("66666666-7777-8888-9999-000000000000"),
        communityId
    );
    repository.save(entity);

    AssetItemTBL updated = AssetItemTBL.create(
        5,
        "New Coupon",
        "COUPON",
        "SUSPENDED",
        UUID.fromString("66666666-7777-8888-9999-000000000000"),
        communityId
    );
    setField(updated, "id", entity.getId());

    int count = repository.updateAssetItem(updated);

    assertThat(count).isEqualTo(1);
    AssetItemTBL found = repository.findById(entity.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("New Coupon");
    assertThat(found.getAmount()).isEqualTo(5);
    assertThat(found.getStatus()).isEqualTo("SUSPENDED");
  }

  @Test
  @DisplayName("커뮤니티 ID로 자산을 조회한다")
  void findsByOwnerCommunityId() {
    UUID communityId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    AssetItemTBL entity = AssetItemTBL.create(
        2,
        "Coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("ffffffff-1111-2222-3333-444444444444"),
        communityId
    );
    repository.save(entity);

    List<AssetItemTBL> items = repository.findByOwnerCommunityId(communityId);

    assertThat(items).hasSize(1);
  }

  @Test
  @DisplayName("없는 커뮤니티 ID로 조회하면 빈 결과를 반환한다")
  void returnsEmptyWhenCommunityMissing() {
    UUID communityId = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");

    List<AssetItemTBL> items = repository.findByOwnerCommunityId(communityId);

    assertThat(items).isEmpty();
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set field: " + name, e);
    }
  }
}
