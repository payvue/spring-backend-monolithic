package com.payvue.supporting.user.command;

import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserFeaturePreferenceEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserMembershipEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserPasswordHistoryEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.account.UserPaymentLimitSetting;
import com.payvue.supporting.user.FeaturePreference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserPersistenceMapper {

  private final ObjectMapper objectMapper;

  public UserPersistenceMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public UserEntity toEntity(User user) {
    if (user == null) {
      return null;
    }
    UserEntity entity = new UserEntity();
    entity.setId(user.getId().getValue().toString());

    Profile profile = user.getProfile();
    Account account = user.getAccount();
    Credentials credentials = user.getCredentials();
    FeaturePreferences featurePreferences = user.getFeaturePreferences();
    Address address = user.getAddress();
    List<Membership> memberships = user.getMemberships();

    if (account != null) {
      entity.setLoginId(account.getLoginId());
      entity.setEmail(account.getEmail());
      entity.setPhoneNumber(account.getPhoneNumber());
      entity.setEmailVerified(account.isEmailVerified());
      entity.setPhoneVerified(account.isPhoneVerified());
    }

    LoginStatus loginStatus = user.getLoginStatus();
    if (loginStatus != null) {
      entity.setLastLoggedIn(loginStatus.getLastLoggedIn());
      entity.setLoggedIn(loginStatus.isLoggedIn());
    }

    if (profile != null) {
      entity.setDisplayName(profile.getNickname());
      entity.setRealName(profile.getRealName());
      entity.setDateOfBirth(profile.getDateOfBirth());
      entity.setProfileImageUrl(profile.getProfileImageUrl());
      entity.setBio(profile.getBio());
    }

    entity.setStatus(user.getStatus());

    if (credentials != null) {
      entity.setPassword(credentials.getCurrent());
      entity.clearPasswordHistories();
      for (String previous : readPasswordHistory(credentials)) {
        UserPasswordHistoryEntity history = new UserPasswordHistoryEntity();
        history.setEncryptedPassword(previous);
        history.setCreatedAt(Instant.now());
        entity.addPasswordHistory(history);
      }
    }

    if (address != null) {
      entity.setSidoCode(address.getSidoCode());
      entity.setSigunguCode(address.getSigunguCode());
      entity.setRoadAddress(address.getRoadAddress());
      entity.setDetailAddress(address.getDetailAddress());
      entity.setPostalCode(address.getPostalCode());
    }

    if (featurePreferences != null) {
      entity.getFeaturePreferences().clear();
      for (Map.Entry<FeatureKey, FeaturePreference> entry : featurePreferences.getValues().entrySet()) {
        UserFeaturePreferenceEntity feature = new UserFeaturePreferenceEntity();
        feature.setFeatureKey(entry.getKey().getValue());
        feature.setPayload(resolvePayload(entry.getValue()));
        entity.getFeaturePreferences().add(feature);
      }
    }

    if (memberships != null) {
      entity.getMemberships().clear();
      for (Membership membership : memberships) {
        UserMembershipEntity membershipEntity = new UserMembershipEntity();
        membershipEntity.setCommunityId(membership.getCommunityId());
        membershipEntity.setJoinedAt(membership.getJoinedAt());
        entity.getMemberships().add(membershipEntity);
      }
    }

    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    return entity;
  }

  public User toDomain(UserEntity entity) {
    if (entity == null) {
      return null;
    }

    UserId userId = UserId.of(UUID.fromString(entity.getId()));
    Profile profile = Profile.of(
        entity.getDisplayName(),
        entity.getRealName(),
        entity.getDateOfBirth(),
        entity.getProfileImageUrl(),
        entity.getBio()
    );
    Account account = Account.of(entity.getLoginId(), entity.getEmail(), entity.getPhoneNumber());
    if (entity.isEmailVerified()) {
      account = account.verifyEmail();
    }
    if (entity.isPhoneVerified()) {
      account = account.verifyPhone();
    }

    List<Membership> memberships = new ArrayList<>();
    for (UserMembershipEntity membershipEntity : entity.getMemberships()) {
      memberships.add(createMembership(membershipEntity.getCommunityId(), membershipEntity.getJoinedAt()));
    }

    FeaturePreferences featurePreferences = FeaturePreferences.empty();
    for (UserFeaturePreferenceEntity feature : entity.getFeaturePreferences()) {
      FeaturePreference preference = parseFeaturePreference(feature);
      if (preference != null) {
        featurePreferences = featurePreferences.apply(preference);
      }
    }

    LoginStatus loginStatus = LoginStatus.of(entity.getLastLoggedIn(), entity.isLoggedIn());

    Credentials credentials = buildCredentials(entity.getPassword(), entity.getPasswordHistories());

    Address address = buildAddress(
        entity.getSidoCode(),
        entity.getSigunguCode(),
        entity.getRoadAddress(),
        entity.getDetailAddress(),
        entity.getPostalCode()
    );

    return User.reconstitute(
        userId,
        profile,
        account,
        credentials,
        entity.getStatus(),
        loginStatus,
        featurePreferences,
        address,
        memberships,
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  private Credentials buildCredentials(String current, List<UserPasswordHistoryEntity> histories) {
    if (current == null && (histories == null || histories.isEmpty())) {
      return Credentials.empty();
    }
    Object history = createPasswordHistory();
    if (histories != null) {
      for (UserPasswordHistoryEntity entry : histories.stream()
          .sorted(Comparator.comparing(UserPasswordHistoryEntity::getCreatedAt))
          .toList()) {
        history = appendPasswordHistory(history, entry.getEncryptedPassword());
      }
    }
    return newCredentials(current, history);
  }

  private Credentials newCredentials(String current, Object history) {
    try {
      Constructor<Credentials> constructor = Credentials.class.getDeclaredConstructor(String.class, history.getClass());
      constructor.setAccessible(true);
      return constructor.newInstance(current, history);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create credentials", e);
    }
  }

  private Object createPasswordHistory() {
    try {
      Class<?> type = Class.forName("com.payvue.supporting.user.command.PasswordHistory");
      Method empty = type.getDeclaredMethod("empty");
      empty.setAccessible(true);
      return empty.invoke(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create password history", e);
    }
  }

  private Object appendPasswordHistory(Object history, String encryptedPassword) {
    try {
      Method append = history.getClass().getDeclaredMethod("append", String.class);
      append.setAccessible(true);
      return append.invoke(history, encryptedPassword);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to append password history", e);
    }
  }

  private List<String> readPasswordHistory(Credentials credentials) {
    try {
      Method getHistory = Credentials.class.getDeclaredMethod("getHistory");
      getHistory.setAccessible(true);
      Object history = getHistory.invoke(credentials);
      Method getPrevious = history.getClass().getDeclaredMethod("getPrevious");
      getPrevious.setAccessible(true);
      Object previous = getPrevious.invoke(history);
      if (previous instanceof List<?> list) {
        List<String> values = new ArrayList<>();
        for (Object item : list) {
          values.add(String.valueOf(item));
        }
        return values;
      }
      return List.of();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read password history", e);
    }
  }

  private Address buildAddress(
      String sidoCode,
      String sigunguCode,
      String roadAddress,
      String detailAddress,
      String postalCode
  ) {
    if (isBlank(sidoCode) || isBlank(sigunguCode) || isBlank(roadAddress) || isBlank(postalCode)) {
      return null;
    }
    return Address.of(sidoCode, sigunguCode, roadAddress, detailAddress, postalCode);
  }

  private Membership createMembership(String communityId, Instant joinedAt) {
    try {
      Constructor<Membership> constructor = Membership.class.getDeclaredConstructor(String.class, Instant.class);
      constructor.setAccessible(true);
      return constructor.newInstance(communityId, joinedAt);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to create membership", e);
    }
  }

  private String resolvePayload(FeaturePreference preference) {
    if (preference instanceof PayloadFeaturePreference payload) {
      return payload.payload();
    }
    if (preference instanceof UserPaymentLimitSetting setting) {
      try {
        return objectMapper.writeValueAsString(setting);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Failed to serialize payment limit setting", e);
      }
    }
    return null;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private FeaturePreference parseFeaturePreference(UserFeaturePreferenceEntity feature) {
    if (feature == null) {
      return null;
    }
    String key = feature.getFeatureKey();
    String payload = feature.getPayload();
    if (UserPaymentLimitSetting.KEY.getValue().equals(key) && !isBlank(payload)) {
      try {
        return objectMapper.readValue(payload, UserPaymentLimitSetting.class);
      } catch (JsonProcessingException e) {
        return new PayloadFeaturePreference(FeatureKey.of(key), payload);
      }
    }
    return new PayloadFeaturePreference(FeatureKey.of(key), payload);
  }

  static final class PayloadFeaturePreference implements FeaturePreference {
    private final FeatureKey key;
    private final String payload;

    PayloadFeaturePreference(FeatureKey key, String payload) {
      this.key = key;
      this.payload = payload;
    }

    @Override
    public FeatureKey key() {
      return key;
    }

    String payload() {
      return payload;
    }
  }
}
