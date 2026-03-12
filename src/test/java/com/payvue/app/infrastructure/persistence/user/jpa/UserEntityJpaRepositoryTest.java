package com.payvue.app.infrastructure.persistence.user.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import com.payvue.supporting.user.command.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
class UserEntityJpaRepositoryTest {

  @Autowired
  private UserEntityJpaRepository repository;

  @Test
  @DisplayName("loginId와 email로 조회한다")
  void findsByLoginIdAndEmail() {
    UserEntity entity = createEntity("user-1", "login-1", "user@payvue.com");
    repository.save(entity);

    assertThat(repository.findByLoginId("login-1")).isPresent();
    assertThat(repository.findByEmail("user@payvue.com")).isPresent();
  }

  @Test
  @DisplayName("존재하지 않는 사용자 조회는 비어 있다")
  void returnsEmptyWhenMissing() {
    assertThat(repository.findByLoginId("missing")).isEmpty();
    assertThat(repository.findByEmail("missing@payvue.com")).isEmpty();
  }

  @Test
  @DisplayName("updateUser는 주요 필드를 갱신한다")
  @Transactional
  @Rollback
  void updatesUserFields() {
    UserEntity entity = createEntity("user-2", "login-2", "before@payvue.com");
    repository.save(entity);

    UserEntity updated = createEntity("user-2", "login-2-updated", "after@payvue.com");
    updated.setDisplayName("after-name");
    updated.setUpdatedAt(Instant.parse("2024-01-02T00:00:00Z"));

    int count = repository.updateUser(updated);

    assertThat(count).isEqualTo(1);
    UserEntity loaded = repository.findById("user-2").orElseThrow();
    assertThat(loaded.getLoginId()).isEqualTo("login-2-updated");
    assertThat(loaded.getEmail()).isEqualTo("after@payvue.com");
    assertThat(loaded.getDisplayName()).isEqualTo("after-name");
  }

  private UserEntity createEntity(String id, String loginId, String email) {
    UserEntity entity = new UserEntity();
    entity.setId(id);
    entity.setLoginId(loginId);
    entity.setEmail(email);
    entity.setPhoneNumber("010-0000-0000");
    entity.setEmailVerified(true);
    entity.setPhoneVerified(false);
    entity.setLastLoggedIn(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setLoggedIn(true);
    entity.setDisplayName("display");
    entity.setRealName("real");
    entity.setDateOfBirth(LocalDate.of(2000, 1, 1));
    entity.setProfileImageUrl("https://example.com/profile.png");
    entity.setBio("bio");
    entity.setStatus(UserStatus.ACTIVE);
    entity.setPassword("hashed");
    entity.setSidoCode("01");
    entity.setSigunguCode("02");
    entity.setRoadAddress("road");
    entity.setDetailAddress("detail");
    entity.setPostalCode("00000");
    entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    return entity;
  }
}
