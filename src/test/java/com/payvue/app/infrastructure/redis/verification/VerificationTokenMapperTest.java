package com.payvue.app.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenMapperTest {

  @Test
  @DisplayName("VerificationToken을 Entity로 매핑한다")
  void mapsToEntity() {
    VerificationKeyGenerator keyGenerator = new VerificationKeyGenerator();
    VerificationTokenMapper mapper = new VerificationTokenMapper(keyGenerator);
    VerificationToken token = VerificationToken.reconstitute(
        "token-1",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of("key", "value")
    );

    VerificationTokenEntity entity = mapper.toEntity(token);

    assertThat(entity.getId()).isEqualTo("verification:SIGNUP:user@payvue.com");
    assertThat(entity.getToken()).isEqualTo("token-1");
    assertThat(entity.getPurpose()).isEqualTo("SIGNUP");
    assertThat(entity.getIdentifier()).isEqualTo("user@payvue.com");
    assertThat(entity.getCreatedAt()).isEqualTo(1000L);
    assertThat(entity.getMetadata()).containsEntry("key", "value");
  }

  @Test
  @DisplayName("Entity를 VerificationToken으로 매핑한다")
  void mapsToDomain() {
    VerificationKeyGenerator keyGenerator = new VerificationKeyGenerator();
    VerificationTokenMapper mapper = new VerificationTokenMapper(keyGenerator);
    VerificationTokenEntity entity = VerificationTokenEntity.builder()
        .id("verification:SIGNUP:user@payvue.com")
        .token("token-1")
        .purpose("SIGNUP")
        .identifier("user@payvue.com")
        .createdAt(1000L)
        .metadata(Map.of("key", "value"))
        .ttl(60L)
        .build();

    VerificationToken token = mapper.toDomain(entity);

    assertThat(token.getToken()).isEqualTo("token-1");
    assertThat(token.getPurpose()).isEqualTo(VerificationPurpose.SIGNUP);
    assertThat(token.getIdentifier()).isEqualTo("user@payvue.com");
    assertThat(token.getCreatedAt()).isEqualTo(Instant.ofEpochSecond(1000));
    assertThat(token.getMetadata()).containsEntry("key", "value");
  }
}
