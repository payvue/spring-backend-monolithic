package com.payvue.supporting.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.account.CommandType;
import com.payvue.supporting.user.command.FeatureKey;
import com.payvue.supporting.user.command.FeaturePreferences;
import com.payvue.supporting.user.command.PasswordPolicyCommand;
import com.payvue.supporting.user.command.PasswordPrincipal;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserRepository;
import com.payvue.supporting.user.query.UserQueryRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
    UserContextConfiguration.class,
    UserContextConfigurationBootTest.StubConfig.class
})
class UserContextConfigurationBootTest {

  @Autowired
  private UserContextManager userContextManager;

  @Test
  @DisplayName("이메일 조회 시 UserAccountView로 매핑된다")
  void findByEmailResolvesView() {
    User user = newUser();
    userContextManager.save(user, CommandType.REGISTER::name);

    String email = PasswordPrincipal.from(user).email();
    Optional<UserAccountView> view = userContextManager.findByEmail(email, UserAccountView.class);

    assertThat(view).isPresent();
    assertThat(view.get().userId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("이메일 조회 시 User로 매핑된다")
  void findByEmailResolvesUser() {
    User user = newUser();
    userContextManager.save(user, CommandType.REGISTER::name);

    String email = PasswordPrincipal.from(user).email();
    Optional<User> view = userContextManager.findByEmail(email, User.class);

    assertThat(view).isPresent();
    assertThat(view.get().getId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("REGISTER 저장 시 기본 기능 설정이 적용된다")
  void registerAppliesDefaults() {
    User user = newUser();
    userContextManager.save(user, CommandType.REGISTER::name);

    FeaturePreferences preferences = user.getFeaturePreferences();
    assertThat(preferences.getValues().keySet())
            .extracting(FeatureKey::getValue)
            .contains("NOTIFICATION_SETTING", "SECURITY_SETTING");
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
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    UserRepository userRepository() {
      return new InMemoryUserRepository();
    }

    @Bean
    UserQueryRepository userQueryRepository() {
      return new InMemoryUserQueryRepository();
    }

    static final class InMemoryUserRepository implements UserRepository {
      User store;

      @Override
      public User save(User user) {
        store = user;
        return user;
      }

      @Override
      public Optional<User> findById(UserId id) {
        return Optional.ofNullable(store);
      }

      @Override
      public Optional<User> findByLoginId(String loginId) {
        return Optional.ofNullable(store);
      }

      @Override
      public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(store);
      }

      @Override
      public Set<UUID> participantedIn(String email) {
        return Set.of();
      }
    }

    static final class InMemoryUserQueryRepository implements UserQueryRepository {
      @Override
      public Optional<UserAccountView> findById(UserId id) {
        return Optional.empty();
      }

      @Override
      public Optional<UserAccountView> findByEmail(String email) {
        return Optional.empty();
      }
    }
  }
}
