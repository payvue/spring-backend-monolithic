package com.payvue.supporting.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.account.CommandType;
import com.payvue.supporting.user.UserCommand;
import com.payvue.supporting.user.command.*;
import com.payvue.supporting.user.query.UserQueryRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserContextConfigurationTest {

  @Test
  @DisplayName("REGISTER 명령 저장 시 기본 기능 설정이 적용된다")
  void saveAppliesDefaultsForRegisterCommand() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserContextManager manager = new UserContextConfiguration()
        .userContextManager(repository, new InMemoryUserQueryRepository());

    User user = newUser();
    manager.save(user, new RegisterPasswordCommand("Abcdef1!", "enc"));

    FeaturePreferences preferences = user.getFeaturePreferences();
    assertThat(preferences.getValues().keySet())
        .extracting(FeatureKey::getValue)
        .contains("NOTIFICATION_SETTING", "SECURITY_SETTING");
  }

  @Test
  @DisplayName("이메일 조회 시 UserAccountView로 매핑된다")
  void findByEmailResolvesUserAccountView() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserContextManager manager = new UserContextConfiguration()
        .userContextManager(repository, new InMemoryUserQueryRepository());

    User user = newUser();
    repository.store = user;

    Optional<UserAccountView> view = manager.findByEmail(PasswordPrincipal.from(user).email(), UserAccountView.class);
    assertThat(view).isPresent();
    assertThat(view.get().userId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("CHANGE_PASSWORD는 비밀번호 정책을 통과하면 저장된다")
  void saveAppliesPolicyForChangePassword() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserContextManager manager = new UserContextConfiguration()
        .userContextManager(repository, new InMemoryUserQueryRepository());

    User user = newUser();
    manager.save(user, new ChangePasswordCommand("Abcdef1!", "enc"));

    assertThat(repository.store).isEqualTo(user);
  }

  @Test
  @DisplayName("CHANGE_PASSWORD는 비밀번호 정책을 통과하지 못하면 저장되지 않는다")
  void saveRejectsInvalidPasswordForChangePassword() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserContextManager manager = new UserContextConfiguration()
        .userContextManager(repository, new InMemoryUserQueryRepository());

    User user = newUser();

    assertThatThrownBy(() -> manager.save(user, new ChangePasswordCommand("short", "enc")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(repository.store).isNull();
  }

  @Test
  @DisplayName("UPDATE_PROFILE은 기본 기능 설정을 적용하지 않는다")
  void saveDoesNotApplyDefaultsForUpdateProfile() {
    InMemoryUserRepository repository = new InMemoryUserRepository();
    UserContextManager manager = new UserContextConfiguration()
        .userContextManager(repository, new InMemoryUserQueryRepository());

    User user = newUser();
    manager.save(user, new UpdateProfileCommand());

    assertThat(repository.store).isEqualTo(user);
    assertThat(user.getFeaturePreferences().getValues()).isEmpty();
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

  static final class RegisterPasswordCommand implements UserCommand, PasswordPolicyCommand {
    private final String raw;
    private final String encrypted;

    RegisterPasswordCommand(String raw, String encrypted) {
      this.raw = raw;
      this.encrypted = encrypted;
    }

    @Override
    public String type() {
      return CommandType.REGISTER.name();
    }

    @Override
    public String rawPassword() {
      return raw;
    }

    @Override
    public String encryptedPassword() {
      return encrypted;
    }
  }

  static final class ChangePasswordCommand implements UserCommand, PasswordPolicyCommand {
    private final String raw;
    private final String encrypted;

    ChangePasswordCommand(String raw, String encrypted) {
      this.raw = raw;
      this.encrypted = encrypted;
    }

    @Override
    public String type() {
      return CommandType.CHANGE_PASSWORD.name();
    }

    @Override
    public String rawPassword() {
      return raw;
    }

    @Override
    public String encryptedPassword() {
      return encrypted;
    }
  }

  static final class UpdateProfileCommand implements UserCommand {
    @Override
    public String type() {
      return CommandType.UPDATE_PROFILE.name();
    }
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
