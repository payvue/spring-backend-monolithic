package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {

  @Test
  @DisplayName("사용자가 없으면 UsernameNotFoundException을 던진다")
  void throwsWhenUserMissing() {
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByEmail("missing@payvue.com")).thenReturn(Optional.empty());

    CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    assertThatThrownBy(() -> service.loadUserByUsername("missing@payvue.com"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  @DisplayName("예외 메시지에 이메일이 포함된다")
  void exceptionMessageContainsEmail() {
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByEmail("missing@payvue.com")).thenReturn(Optional.empty());

    CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    assertThatThrownBy(() -> service.loadUserByUsername("missing@payvue.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@payvue.com");
  }

  @Test
  @DisplayName("사용자가 있으면 AuthUserDetails를 반환한다")
  void returnsAuthUserDetails() {
    UserRepository userRepository = mock(UserRepository.class);
    User user = User.create(
        "login",
        "user@payvue.com",
        "010-0000-0000",
        "nick",
        "real",
        LocalDate.of(2000, 1, 1),
        "01",
        "02",
        "road",
        "detail",
        "00000",
        "hashed",
        mock(com.payvue.supporting.user.command.PasswordPolicy.class)
    );
    when(userRepository.findByEmail("user@payvue.com")).thenReturn(Optional.of(user));

    CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    UserDetails details = service.loadUserByUsername("user@payvue.com");

    assertThat(details.getUsername()).isEqualTo("user@payvue.com");
    assertThat(details.getPassword()).isEqualTo("hashed");
  }

}
