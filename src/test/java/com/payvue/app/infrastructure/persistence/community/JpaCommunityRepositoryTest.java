package com.payvue.app.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaCommunityRepositoryTest.TestConfig.class)
class JpaCommunityRepositoryTest {

  @TestConfiguration
  static class TestConfig {

    @Bean
    RoleCatalog roleCatalog() {
      return RoleCatalog.builder()
          .register(Role.ADMIN)
          .register(Role.MEMBER)
          .register(Role.GUEST)
          .build();
    }

    @Bean
    CommunityEntityMapper communityEntityMapper() {
      return new CommunityEntityMapperImpl();
    }

    @Bean
    JpaCommunityRepository jpaCommunityRepository(
        CommunityJpaRepository communityJpaRepository,
        CommunityEntityMapper communityEntityMapper
    ) {
      return new JpaCommunityRepository(communityJpaRepository, communityEntityMapper);
    }
  }

  @Autowired
  private JpaCommunityRepository repository;

  @Autowired
  private RoleCatalog roleCatalog;

  @Test
  @DisplayName("커뮤니티를 저장하고 조회한다")
  void savesAndFindsCommunity() {
    UUID adminId = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
    Community community = Cooperative.create(
        "Org",
        "desc",
        adminId,
        roleCatalog.admin(),
        roleCatalog.member()
    );

    Community saved = repository.save(community);

    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findById(saved.getId())).isPresent();
    assertThat(repository.existsById(saved.getId())).isTrue();
  }

  @Test
  @DisplayName("커뮤니티 ID 목록으로 조회한다")
  void findsByIds() {
    UUID adminId = UUID.fromString("cccccccc-1111-2222-3333-dddddddddddd");
    Community community = Cooperative.create(
        "Org",
        "desc",
        adminId,
        roleCatalog.admin(),
        roleCatalog.member()
    );
    Community saved = repository.save(community);

    List<Community> communities = repository.findAllByIds(Set.of(saved.getId()));

    assertThat(communities).hasSize(1);
    assertThat(communities.get(0).getId()).isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("ID 목록이 비어 있으면 빈 결과를 반환한다")
  void returnsEmptyWhenIdsEmpty() {
    List<Community> communities = repository.findAllByIds(Set.of());

    assertThat(communities).isEmpty();
  }

  @Test
  @DisplayName("이름으로 커뮤니티 존재 여부를 확인한다")
  void existsByName() {
    UUID adminId = UUID.fromString("eeeeeeee-1111-2222-3333-ffffffffffff");
    Community community = Cooperative.create(
        "Org",
        "desc",
        adminId,
        roleCatalog.admin(),
        roleCatalog.member()
    );
    repository.save(community);

    assertThat(repository.existsByName("Org")).isTrue();
    assertThat(repository.existsByName("Unknown")).isFalse();
  }

  @Test
  @DisplayName("동일한 ID의 커뮤니티를 업데이트한다")
  void updatesExistingCommunity() {
    UUID adminId = UUID.fromString("11111111-aaaa-bbbb-cccc-222222222222");
    Community community = Cooperative.create(
        "Org",
        "desc",
        adminId,
        roleCatalog.admin(),
        roleCatalog.member()
    );
    Community saved = repository.save(community);

    Community updated = Cooperative.create(
        "Updated",
        "new desc",
        adminId,
        roleCatalog.admin(),
        roleCatalog.member()
    );
    setField(updated, "id", saved.getId());

    Community result = repository.save(updated);

    assertThat(result.getId()).isEqualTo(saved.getId());
    assertThat(repository.findById(saved.getId())).isPresent();
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field field = target.getClass().getSuperclass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set field: " + name, e);
    }
  }
}
