package com.payvue.supporting.kyc;

import com.payvue.app.infrastructure.persistence.user.entity.UserKycEntity;
import com.payvue.supporting.user.command.UserId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserKycMapper {

  public UserKycEntity toEntity(UserKyc domain) {
    if (domain == null) {
      return null;
    }
    UserKycEntity entity = new UserKycEntity();
    entity.setUserId(domain.getUserId().getValue().toString());
    entity.setResidentRegistrationNo(domain.getEncryptedResidentRegistrationNo());
    entity.setStatus(domain.getStatus());
    entity.setVerifiedAt(domain.getVerifiedAt());
    return entity;
  }

  public UserKyc toDomain(UserKycEntity entity) {
    if (entity == null) {
      return null;
    }
    UserKyc pending = UserKyc.pending(
        UserId.of(UUID.fromString(entity.getUserId())),
        entity.getResidentRegistrationNo()
    );
    if (entity.getStatus() == KycStatus.VERIFIED) {
      return pending.verify(entity.getVerifiedAt());
    }
    if (entity.getStatus() == KycStatus.REJECTED) {
      return pending.verify(entity.getVerifiedAt()).reject();
    }
    return pending;
  }
}
