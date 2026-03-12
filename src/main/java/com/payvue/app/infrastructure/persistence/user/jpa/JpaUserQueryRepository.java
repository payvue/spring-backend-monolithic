package com.payvue.app.infrastructure.persistence.user.jpa;

import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import com.payvue.app.infrastructure.persistence.user.entity.UserMembershipEntity;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.query.UserQueryRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserQueryRepository implements UserQueryRepository {

  private final UserEntityJpaRepository userEntityJpaRepository;

  public JpaUserQueryRepository(UserEntityJpaRepository userEntityJpaRepository) {
    this.userEntityJpaRepository = userEntityJpaRepository;
  }

  @Override
  public Optional<UserAccountView> findById(UserId id) {
    if (id == null) {
      return Optional.empty();
    }
    return userEntityJpaRepository.findById(id.getValue().toString()).map(this::toView);
  }

  @Override
  public Optional<UserAccountView> findByEmail(String email) {
    if (email == null || email.isBlank()) {
      return Optional.empty();
    }
    return userEntityJpaRepository.findByEmail(email).map(this::toView);
  }

  private UserAccountView toView(UserEntity entity) {
    if (entity == null) {
      return new UserAccountView(
          null, null, null, null, false, null, null, null, false, null, Map.of()
      );
    }
    Map<UUID, java.time.Instant> memberships = new HashMap<>();
    for (UserMembershipEntity membership : entity.getMemberships()) {
      if (membership == null || membership.getCommunityId() == null) {
        continue;
      }
      memberships.put(
          UUID.fromString(membership.getCommunityId()),
          membership.getJoinedAt()
      );
    }
    return new UserAccountView(
        UserId.of(UUID.fromString(entity.getId())),
        entity.getLoginId(),
        entity.getEmail(),
        entity.getPhoneNumber(),
        entity.isEmailVerified(),
        entity.getRealName(),
        entity.getDisplayName(),
        entity.getStatus(),
        entity.isLoggedIn(),
        entity.getLastLoggedIn(),
        memberships
    );
  }
}
