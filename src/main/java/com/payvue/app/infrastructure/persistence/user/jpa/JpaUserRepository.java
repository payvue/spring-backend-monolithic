package com.payvue.app.infrastructure.persistence.user.jpa;

import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserPersistenceMapper;
import com.payvue.supporting.user.command.UserRepository;
import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaUserRepository implements UserRepository {

  private final UserEntityJpaRepository userEntityJpaRepository;
  private final UserPersistenceMapper mapper;

  public JpaUserRepository(UserEntityJpaRepository userEntityJpaRepository, UserPersistenceMapper mapper) {
    this.userEntityJpaRepository = userEntityJpaRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public User save(User user) {
    UserEntity entity = mapper.toEntity(user);
    UserEntity saved = userEntityJpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return userEntityJpaRepository.findById(id.getValue().toString()).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByLoginId(String loginId) {
    return userEntityJpaRepository.findByLoginId(loginId).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userEntityJpaRepository.findByEmail(email).map(mapper::toDomain);
  }

  @Override
  public Set<UUID> participantedIn(String email) {
    return findByEmail(email)
        .map(UserAccountView::from)
        .map(view -> view.membershipsByCommunity().keySet().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()))
        .orElseGet(Set::of);
  }
}
