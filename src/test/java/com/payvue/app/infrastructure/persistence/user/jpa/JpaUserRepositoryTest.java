package com.payvue.app.infrastructure.persistence.user.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.payvue.supporting.user.command.PasswordPolicy;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserPersistenceMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest
@Import({JpaUserRepository.class, UserPersistenceMapper.class, JpaUserRepositoryTest.TestConfig.class})
class JpaUserRepositoryTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }

  @Autowired
  private JpaUserRepository repository;

  @Test
  @DisplayName("사용자를 저장하고 조회한다")
  void savesAndFindsUser() {
    User user = createUser("login-1", "user@payvue.com");

    User saved = repository.save(user);

    assertThat(repository.findById(saved.getId())).isPresent();
    assertThat(repository.findByLoginId("login-1")).isPresent();
    assertThat(repository.findByEmail("user@payvue.com")).isPresent();
  }

  @Test
  @DisplayName("존재하지 않는 사용자 조회는 비어 있다")
  void returnsEmptyWhenUserMissing() {
    assertThat(repository.findByEmail("missing@payvue.com")).isEmpty();
    assertThat(repository.findByLoginId("missing-login")).isEmpty();
  }

  @Test
  @DisplayName("커뮤니티 참여 목록은 멤버십에서 조회된다")
  void participantedInReturnsMemberships() {
    User user = createUser("login-2", "member@payvue.com");
    user.activate();
    String communityIdOne = "11111111-1111-1111-1111-111111111111";
    String communityIdTwo = "22222222-2222-2222-2222-222222222222";
    user.joinCommunity(communityIdOne);
    user.joinCommunity(communityIdTwo);
    repository.save(user);

    Set<UUID> communities = repository.participantedIn("member@payvue.com");

    assertThat(communities)
        .contains(UUID.fromString(communityIdOne))
        .contains(UUID.fromString(communityIdTwo));
  }

  private User createUser(String loginId, String email) {
    PasswordPolicy policy = mock(PasswordPolicy.class);
    User user = User.create(
        loginId,
        email,
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
        policy
    );
    user.updateAddress("01", "02", "road", "detail", "00000");
    user.markLoggedIn(Instant.parse("2024-01-01T00:00:00Z"));
    user.changePassword("hashed-next", policy);
    return user;
  }
}
