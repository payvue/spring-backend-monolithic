package com.payvue.supporting.user.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.supporting.user.UserCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  @Test
  @DisplayName("기본 비밀번호 정책은 길이, 공백, 문자 클래스 규칙을 검증한다")
  void defaultPasswordPolicyValidatesRules() {
    PasswordHistory history = PasswordHistory.empty();
    DefaultPasswordPolicy policy = DefaultPasswordPolicy.defaults("Abcdef1!");
    policy.validate("enc", history);

    assertThatThrownBy(() -> DefaultPasswordPolicy.defaults("Ab1!").validate("enc", history))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> DefaultPasswordPolicy.defaults("Abcdef1! ").validate("enc", history))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> DefaultPasswordPolicy.defaults("Abcdefgh").validate("enc", history))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("기본 비밀번호 정책은 이전에 사용한 비밀번호를 거부한다")
  void defaultPasswordPolicyRejectsPasswordHistory() {
    PasswordHistory history = PasswordHistory.empty().append("enc");
    assertThatThrownBy(() -> DefaultPasswordPolicy.defaults("Abcdef1!").validate("enc", history))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("UserPasswordGuard는 신규 비밀번호와 재사용 비밀번호를 검증한다")
  void userPasswordGuardValidatesAndRejectsReuse() {
    User user = User.create(
        "login",
        "a@b.com",
        "010",
        "nick",
        "real",
        java.time.LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345",
        "enc-1",
        DefaultPasswordPolicy.defaults("Abcdef1!")
    );

    UserPasswordGuard.validateNew("Abcdef1!", "enc-2");
    user.changePassword("enc-2", DefaultPasswordPolicy.defaults("Abcdef1!"));

    assertThatThrownBy(() -> UserPasswordGuard.ensureNotReused(user, "enc-2"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UserPasswordGuard.ensureNotReused(user, "enc-1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("PasswordPolicySpec은 PasswordPolicyCommand에만 정책을 적용한다")
  void passwordPolicySpecAppliesOnlyToPasswordCommands() {
    PasswordPolicySpec spec = new PasswordPolicySpec(DefaultPasswordPolicy::defaults);

    boolean ignored = spec.validate(() -> "OTHER");
    assertThat(ignored).isTrue();

    boolean invalid = spec.validate(new TestPasswordCommand("", "enc"));
    assertThat(invalid).isFalse();

    boolean valid = spec.validate(new TestPasswordCommand("Abcdef1!", "enc"));
    assertThat(valid).isTrue();
  }

  static final class TestPasswordCommand implements UserCommand, PasswordPolicyCommand {
    private final String raw;
    private final String encrypted;

    TestPasswordCommand(String raw, String encrypted) {
      this.raw = raw;
      this.encrypted = encrypted;
    }

    @Override
    public String type() {
      return "REGISTER";
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
}
