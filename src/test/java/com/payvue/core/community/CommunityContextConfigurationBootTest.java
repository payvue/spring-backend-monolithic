package com.payvue.core.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = {
    CommunityContextConfiguration.class,
    CommunityContextConfigurationBootTest.StubConfig.class
})
class CommunityContextConfigurationBootTest {

  @Autowired
  private CommunityContextManager communityContextManager;

  @Test
  @DisplayName("커뮤니티 컨텍스트는 카테고리에 맞는 커뮤니티를 초기화한다")
  void initCreatesCommunityByCategory() {
    User user = newUser();
    UserAccountView view = UserAccountView.from(user);

    Community personal = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.PERSONAL.name(), view)
    );
    Community organization = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.COOPERATIVE.name(), view)
    );

    assertThat(personal).isInstanceOf(Personal.class);
    assertThat(organization).isInstanceOf(Cooperative.class);
  }

  @Test
  @DisplayName("get은 카테고리와 ID가 일치하는 커뮤니티를 반환한다")
  void getReturnsMatchingCommunity() {
    User user = newUser();
    Community created = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.PERSONAL.name(), UserAccountView.from(user))
    );

    Community found = communityContextManager.get(
        new CategoryQuery(CommunityCategory.PERSONAL.name(), created.getId())
    );

    assertThat(found.getId()).isEqualTo(created.getId());
    assertThat(found.getCommunityType()).isEqualTo(CommunityCategory.PERSONAL.name());
  }

  @Test
  @DisplayName("resolve는 사용자 멤버십에 해당하는 커뮤니티를 반환한다")
  void resolveUsesMemberships() {
    User user = newUser();
    user.activate();

    Community community = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.PERSONAL.name(), UserAccountView.from(user))
    );
    user.joinCommunity(community.getId().getValue().toString());

    Set<Community> resolved = communityContextManager.resolve(user);

    assertThat(resolved).contains(community);
  }

  @Test
  @DisplayName("resolve는 카테고리 필터를 적용한다")
  void resolveFiltersByCategory() {
    User user = newUser();
    user.activate();

    Community personal = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.PERSONAL.name(), UserAccountView.from(user))
    );
    Community cooperative = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.COOPERATIVE.name(), UserAccountView.from(user))
    );
    user.joinCommunity(personal.getId().getValue().toString());
    user.joinCommunity(cooperative.getId().getValue().toString());

    Set<Community> resolved = communityContextManager.resolve(user, CommunityCategory.PERSONAL.name());

    assertThat(resolved).contains(personal);
    assertThat(resolved).doesNotContain(cooperative);
  }

  @Test
  @DisplayName("get은 등록되지 않은 카테고리이면 예외가 발생한다")
  void getThrowsWhenCategoryMissing() {
    assertThatThrownBy(() -> communityContextManager.get(
        new CategoryQuery("MISSING", CommunityId.generate())
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("get은 카테고리가 일치하지 않으면 예외가 발생한다")
  void getThrowsWhenCategoryMismatch() {
    User user = newUser();
    Community created = communityContextManager.init(
        new CommunityInitCommand(CommunityCategory.PERSONAL.name(), UserAccountView.from(user))
    );

    assertThatThrownBy(() -> communityContextManager.get(
        new CategoryQuery(CommunityCategory.COOPERATIVE.name(), created.getId())
    )).isInstanceOf(IllegalArgumentException.class);
  }

  private User newUser() {
    return User.create(
        "login",
        "a@b.com",
        "010",
        "nick",
        "real",
        LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345"
    );
  }

  @TestConfiguration
  static class StubConfig {
    @Bean
    CommunityRepository communityRepository() {
      return new InMemoryCommunityRepository();
    }

    @Bean
    UserRepository userRepository() {
      return new InMemoryUserRepository();
    }

    static final class InMemoryCommunityRepository implements CommunityRepository {
      private final Map<CommunityId, Community> store = new java.util.HashMap<>();

      @Override
      public Community save(Community community) {
        store.put(community.getId(), community);
        return community;
      }

      @Override
      public Optional<Community> findById(CommunityId id) {
        return Optional.ofNullable(store.get(id));
      }

      @Override
      public java.util.List<Community> findAllByIds(Set<CommunityId> ids) {
        return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
      }

      @Override
      public void delete(Community community) {
        store.remove(community.getId());
      }

      @Override
      public boolean existsById(CommunityId id) {
        return store.containsKey(id);
      }

      @Override
      public boolean existsByName(String name) {
        return store.values().stream().anyMatch(c -> c.getName().equals(name));
      }
    }

    static final class InMemoryUserRepository implements UserRepository {
      @Override
      public User save(User user) {
        return user;
      }

      @Override
      public Optional<User> findById(UserId id) {
        return Optional.empty();
      }

      @Override
      public Optional<User> findByLoginId(String loginId) {
        return Optional.empty();
      }

      @Override
      public Optional<User> findByEmail(String email) {
        return Optional.empty();
      }

      @Override
      public Set<UUID> participantedIn(String email) {
        return Set.of();
      }
    }
  }
}
