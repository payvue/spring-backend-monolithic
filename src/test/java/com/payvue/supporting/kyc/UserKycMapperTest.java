package com.payvue.supporting.kyc;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.infrastructure.persistence.user.entity.UserKycEntity;
import com.payvue.supporting.user.command.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserKycMapperTest {

  @Test
  @DisplayName("KYC 도메인은 엔티티로 변환된다")
  void toEntityMapsFields() {
    UserKyc domain = UserKyc.pending(UserId.of(UUID.randomUUID()), "enc-rrn")
        .verify(Instant.now());
    UserKycMapper mapper = new UserKycMapper();

    UserKycEntity entity = mapper.toEntity(domain);

    assertThat(entity.getUserId()).isEqualTo(domain.getUserId().getValue().toString());
    assertThat(entity.getResidentRegistrationNo()).isEqualTo(domain.getEncryptedResidentRegistrationNo());
    assertThat(entity.getStatus()).isEqualTo(domain.getStatus());
    assertThat(entity.getVerifiedAt()).isEqualTo(domain.getVerifiedAt());
  }

  @Test
  @DisplayName("KYC 엔티티는 상태에 따라 도메인으로 변환된다")
  void toDomainMapsStatus() {
    UserKycMapper mapper = new UserKycMapper();
    String userId = UUID.randomUUID().toString();
    Instant now = Instant.now();

    UserKycEntity pending = new UserKycEntity();
    pending.setUserId(userId);
    pending.setResidentRegistrationNo("enc-rrn");
    pending.setStatus(KycStatus.PENDING);
    UserKyc pendingDomain = mapper.toDomain(pending);
    assertThat(pendingDomain.getStatus()).isEqualTo(KycStatus.PENDING);
    assertThat(pendingDomain.getVerifiedAt()).isNull();

    UserKycEntity verified = new UserKycEntity();
    verified.setUserId(userId);
    verified.setResidentRegistrationNo("enc-rrn");
    verified.setStatus(KycStatus.VERIFIED);
    verified.setVerifiedAt(now);
    UserKyc verifiedDomain = mapper.toDomain(verified);
    assertThat(verifiedDomain.getStatus()).isEqualTo(KycStatus.VERIFIED);
    assertThat(verifiedDomain.getVerifiedAt()).isEqualTo(now);

    UserKycEntity rejected = new UserKycEntity();
    rejected.setUserId(userId);
    rejected.setResidentRegistrationNo("enc-rrn");
    rejected.setStatus(KycStatus.REJECTED);
    rejected.setVerifiedAt(now);
    UserKyc rejectedDomain = mapper.toDomain(rejected);
    assertThat(rejectedDomain.getStatus()).isEqualTo(KycStatus.REJECTED);
    assertThat(rejectedDomain.getVerifiedAt()).isEqualTo(now);
  }
}
