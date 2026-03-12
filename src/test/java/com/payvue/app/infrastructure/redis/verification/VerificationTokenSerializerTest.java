package com.payvue.app.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenSerializerTest {

  @Test
  @DisplayName("VerificationToken을 JSON으로 직렬화하고 역직렬화한다")
  void serializeAndDeserialize() {
    ObjectMapper mapper = new ObjectMapper();
    VerificationTokenSerializer serializer = new VerificationTokenSerializer(mapper);
    VerificationToken token = VerificationToken.reconstitute(
        "token-1",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of("key", "value")
    );

    String json = serializer.serialize(token);
    VerificationToken restored = serializer.deserialize(json, VerificationPurpose.SIGNUP, "user@payvue.com");

    assertThat(restored.getToken()).isEqualTo("token-1");
    assertThat(restored.getPurpose()).isEqualTo(VerificationPurpose.SIGNUP);
    assertThat(restored.getIdentifier()).isEqualTo("user@payvue.com");
    assertThat(restored.getMetadata()).containsEntry("key", "value");
  }

  @Test
  @DisplayName("잘못된 JSON은 예외를 던진다")
  void throwsOnInvalidJson() {
    VerificationTokenSerializer serializer = new VerificationTokenSerializer(new ObjectMapper());

    assertThatThrownBy(() -> serializer.deserialize("{invalid}", VerificationPurpose.SIGNUP, "user"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("토큰 역직렬화 실패");
  }
}
