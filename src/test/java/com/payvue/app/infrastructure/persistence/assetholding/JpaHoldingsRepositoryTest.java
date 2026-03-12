package com.payvue.app.infrastructure.persistence.assetholding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.asset.category.Coupon;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsId;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaHoldingsRepositoryTest.TestConfig.class)
class JpaHoldingsRepositoryTest {

  @TestConfiguration
  @AutoConfigurationPackage
  static class TestConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    AssetCharacteristicMapper assetCapabilityMapper(ObjectMapper objectMapper) {
      return new AssetCharacteristicMapper(objectMapper);
    }

    @Bean
    AssetItemMapper assetItemMapper(AssetCharacteristicMapper assetCapabilityMapper) {
      return new AssetItemMapper(assetCapabilityMapper);
    }

    @Bean
    JpaHoldingsRepository jpaHoldingsRepository(
        HoldingsJpaRepository holdingsJpaRepository,
        AssetItemJpaRepository assetItemJpaRepository,
        AssetItemCapabilityJpaRepository assetItemCapabilityJpaRepository,
        AssetItemMapper mapper
    ) {
      return new JpaHoldingsRepository(
          holdingsJpaRepository,
          assetItemJpaRepository,
          assetItemCapabilityJpaRepository,
          mapper
      );
    }
  }

  @Autowired
  private JpaHoldingsRepository repository;

  @Autowired
  private HoldingsJpaRepository holdingsJpaRepository;

  @Autowired
  private AssetItemJpaRepository assetItemJpaRepository;

  @Test
  @DisplayName("Holdings ID 존재 여부를 확인한다")
  void existsById() {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID communityId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    HoldingsTBL entity = HoldingsTBL.create(id, communityId);
    holdingsJpaRepository.save(entity);

    assertThat(repository.existsById(HoldingsId.of(id))).isTrue();
  }

  @Test
  @DisplayName("커뮤니티별 보유 자산을 조회한다")
  void findsByCommunity() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    Community community = Cooperative.create(
        "Org",
        "desc",
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        roleCatalog.admin(),
        roleCatalog.member()
    );

    AssetItemTBL assetEntity = AssetItemTBL.create(
        1,
        "Coupon",
        "COUPON",
        "ACTIVE",
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        community.getId().getValue()
    );
    assetItemJpaRepository.save(assetEntity);

    Holdings holdings = repository.findByCommunity(community);

    assertThat(holdings.getCommunityId()).isEqualTo(community.getId());
    assertThat(holdings.getAssets()).hasSize(1);
  }

  @Test
  @DisplayName("자산이 없어도 Holdings는 생성된다")
  void findsByCommunityWhenNoAssets() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    Community community = Cooperative.create(
        "Org",
        "desc",
        UUID.fromString("77777777-7777-7777-7777-777777777777"),
        roleCatalog.admin(),
        roleCatalog.member()
    );

    Holdings holdings = repository.findByCommunity(community);

    assertThat(holdings.getCommunityId()).isEqualTo(community.getId());
    assertThat(holdings.getAssets()).isEmpty();
  }

  @Test
  @DisplayName("Holdings와 자산을 함께 저장한다")
  void savesHoldingsAndAssets() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    Community community = Cooperative.create(
        "Org",
        "desc",
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        roleCatalog.admin(),
        roleCatalog.member()
    );
    CommunityId communityId = community.getId();
    AssetItem asset = Coupon.create(
        communityId,
        UserId.of(UUID.fromString("66666666-6666-6666-6666-666666666666")),
        "Coupon"
    ).with(Consumable.multipleUse(2));

    Holdings holdings = Holdings.create(community, List.of(asset));

    repository.save(holdings);

    assertThat(holdingsJpaRepository.findById(holdings.getId().getValue())).isPresent();
    assertThat(assetItemJpaRepository.findByOwnerCommunityId(communityId.getValue())).hasSize(1);
  }
}
