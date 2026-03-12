package com.payvue.app.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.app.domain.auth.VerificationPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationKeyGeneratorTest {

  private final VerificationKeyGenerator generator = new VerificationKeyGenerator();

  @Test
  @DisplayName("키와 인덱스는 규칙에 따라 생성된다")
  void generatesKeys() {
    String key = generator.generateKey(VerificationPurpose.SIGNUP, "user@payvue.com");
    String indexKey = generator.generateTokenIndexKey("token");
    String indexValue = generator.generateIndexValue(VerificationPurpose.SIGNUP, "user@payvue.com");

    assertThat(key).isEqualTo("verification:SIGNUP:user@payvue.com");
    assertThat(indexKey).isEqualTo("verification:token:token");
    assertThat(indexValue).isEqualTo("SIGNUP:user@payvue.com");
  }

  @Test
  @DisplayName("역색인 값 파싱은 purpose와 identifier를 반환한다")
  void parsesIndexValue() {
    VerificationKeyGenerator.ParsedIndexValue parsed = generator.parseIndexValue("SIGNUP:user@payvue.com");

    assertThat(parsed.purpose()).isEqualTo(VerificationPurpose.SIGNUP);
    assertThat(parsed.identifier()).isEqualTo("user@payvue.com");
  }

  @Test
  @DisplayName("역색인 값 형식이 다르면 예외를 던진다")
  void parseThrowsOnInvalidFormat() {
    assertThatThrownBy(() -> generator.parseIndexValue("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("잘못된 역색인 형식");
  }
}
