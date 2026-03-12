package com.payvue.supporting.user.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserFeaturePreferenceEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserMembershipEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserPasswordHistoryEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserPersistenceMapperTest {

  @Test
  @DisplayName("User를 UserEntity로 매핑한다")
  void mapsToEntity() {
    UserPersistenceMapper mapper = new UserPersistenceMapper(new ObjectMapper());
    PasswordPolicy policy = mock(PasswordPolicy.class);
    User user = User.create(
        "login-1",
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
        policy
    );
    user.activate();
    user.joinCommunity("11111111-1111-1111-1111-111111111111");
    user.changePassword("hashed-next", policy);
    user.updateAddress("01", "02", "road", "detail", "00000");

    UserEntity entity = mapper.toEntity(user);

    assertThat(entity.getLoginId()).isEqualTo("login-1");
    assertThat(entity.getEmail()).isEqualTo("user@payvue.com");
    assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(entity.getMemberships()).hasSize(1);
    assertThat(entity.getPasswordHistories()).isNotEmpty();
  }

  @Test
  @DisplayName("UserEntity를 User로 매핑한다")
  void mapsToDomain() {
    UserPersistenceMapper mapper = new UserPersistenceMapper(new ObjectMapper());
    UserEntity entity = new UserEntity();
    entity.setId("11111111-1111-1111-1111-111111111111");
    entity.setLoginId("login-2");
    entity.setEmail("user2@payvue.com");
    entity.setPhoneNumber("010-0000-0000");
    entity.setEmailVerified(true);
    entity.setPhoneVerified(false);
    entity.setLastLoggedIn(Instant.parse("2024-01-01T00:00:00Z"));
    entity.setLoggedIn(true);
    entity.setDisplayName("nick");
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
    entity.setUpdatedAt(Instant.parse("2024-01-02T00:00:00Z"));

    UserMembershipEntity membership = new UserMembershipEntity();
    membership.setCommunityId("22222222-2222-2222-2222-222222222222");
    membership.setJoinedAt(Instant.parse("2024-01-03T00:00:00Z"));
    entity.getMemberships().add(membership);

    UserPasswordHistoryEntity history = new UserPasswordHistoryEntity();
    history.setEncryptedPassword("hashed-prev");
    history.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    entity.getPasswordHistories().add(history);

    UserFeaturePreferenceEntity feature = new UserFeaturePreferenceEntity();
    feature.setFeatureKey("pref-1");
    feature.setPayload("on");
    entity.getFeaturePreferences().add(feature);

    User user = mapper.toDomain(entity);

    assertThat(user.getAccount().getLoginId()).isEqualTo("login-2");
    assertThat(user.getAccount().isEmailVerified()).isTrue();
    assertThat(user.isMemberOf("22222222-2222-2222-2222-222222222222")).isTrue();
    assertThat(user.getCredentials().hasPassword()).isTrue();
    assertThat(user.getFeaturePreferences().get(FeatureKey.of("pref-1")).isPresent()).isTrue();
  }
}
