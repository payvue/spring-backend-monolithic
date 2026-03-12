package com.payvue.app.infrastructure.persistence.user.jpa;

import com.payvue.app.infrastructure.persistence.user.entity.UserKycEntity;
import com.payvue.supporting.kyc.UserKyc;
import com.payvue.supporting.kyc.UserKycMapper;
import com.payvue.supporting.kyc.UserKycRepository;
import com.payvue.supporting.user.command.UserId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserKycRepository implements UserKycRepository {

  private final UserKycJpaRepository userKycJpaRepository;
  private final UserKycMapper mapper;

  public JpaUserKycRepository(UserKycJpaRepository userKycJpaRepository, UserKycMapper mapper) {
    this.userKycJpaRepository = userKycJpaRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public UserKyc save(UserKyc userKyc) {
    UserKycEntity entity = mapper.toEntity(userKyc);
    if (entity.getUserId() != null && userKycJpaRepository.existsById(entity.getUserId())) {
      userKycJpaRepository.updateKyc(entity);
      return userKyc;
    }
    UserKycEntity saved = userKycJpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<UserKyc> findByUserId(UserId userId) {
    return userKycJpaRepository.findByUserId(userId.getValue().toString()).map(mapper::toDomain);
  }

  @Override
  public void deleteByUserId(UserId userId) {
    if (userId == null) {
      return;
    }
    userKycJpaRepository.deleteById(userId.getValue().toString());
  }
}
