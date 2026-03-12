package com.payvue.app.infrastructure.redis.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.app.domain.auth.VerificationTokenRepository;
import com.payvue.app.infrastructure.redis.RedisContainerSupport;
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(classes = {
    RedisAutoConfiguration.class,
    RedisRepositoryConfiguration.class,
    VerificationKeyGenerator.class,
    VerificationTokenMapper.class,
    VerificationTokenSerializer.class,
    RedisVerificationTokenRepository.class,
    RedisVerificationTokenRepositoryTest.RedisRepoConfig.class
})
class RedisVerificationTokenRepositoryTest extends RedisContainerSupport {

  @Autowired
  private VerificationTokenRepository repository;

  @Autowired
  private StringRedisTemplate template;

  @Autowired
  private VerificationKeyGenerator keyGenerator;

  @Test
  @DisplayName("save는 역색인 키를 저장한다")
  void saveStoresIndexKey() {
    VerificationToken token = VerificationToken.reconstitute(
        "token-1",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of("k", "v")
    );

    repository.save(token);

    String indexKey = keyGenerator.generateTokenIndexKey("token-1");
    assertThat(template.opsForValue().get(indexKey))
        .isEqualTo("SIGNUP:user@payvue.com");
  }

  @Test
  @DisplayName("findByToken은 역색인에서 목적이 일치하면 토큰을 반환한다")
  void findByTokenReturnsMatchingPurpose() {
    VerificationToken token = VerificationToken.reconstitute(
        "token-2",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of()
    );
    repository.save(token);

    Optional<VerificationToken> found = repository.findByToken("token-2", VerificationPurpose.SIGNUP);

    assertThat(found).isPresent();
    assertThat(found.get().getIdentifier()).isEqualTo("user@payvue.com");
  }

  @Test
  @DisplayName("findByToken은 목적이 다르면 비어있는 결과를 반환한다")
  void findByTokenReturnsEmptyWhenPurposeMismatch() {
    VerificationToken token = VerificationToken.reconstitute(
        "token-3",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of()
    );
    repository.save(token);

    Optional<VerificationToken> found = repository.findByToken("token-3", VerificationPurpose.RESET_PASSWORD);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("findByToken은 엔티티가 없으면 역색인을 삭제한다")
  void findByTokenDeletesIndexWhenEntityMissing() {
    String indexKey = keyGenerator.generateTokenIndexKey("token-missing");
    template.opsForValue().set(indexKey, "SIGNUP:user@payvue.com");

    Optional<VerificationToken> found = repository.findByToken("token-missing", VerificationPurpose.SIGNUP);

    assertThat(found).isEmpty();
    assertThat(template.hasKey(indexKey)).isFalse();
  }

  @Test
  @DisplayName("delete는 엔티티와 역색인 키를 삭제한다")
  void deleteRemovesEntityAndIndex() {
    VerificationToken token = VerificationToken.reconstitute(
        "token-4",
        VerificationPurpose.SIGNUP,
        "user@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of()
    );
    repository.save(token);

    repository.delete("user@payvue.com", VerificationPurpose.SIGNUP);

    String indexKey = keyGenerator.generateTokenIndexKey("token-4");
    assertThat(template.hasKey(indexKey)).isFalse();
    assertThat(repository.exists("user@payvue.com", VerificationPurpose.SIGNUP)).isFalse();
  }

  @Test
  @DisplayName("exists는 저장된 토큰이 있으면 true를 반환한다")
  void existsReturnsTrueWhenSaved() {
    VerificationToken token = VerificationToken.reconstitute(
        "token-5",
        VerificationPurpose.SIGNUP,
        "exists@payvue.com",
        Instant.ofEpochSecond(1000),
        Map.of()
    );
    repository.save(token);

    assertThat(repository.exists("exists@payvue.com", VerificationPurpose.SIGNUP)).isTrue();
  }

  @Configuration
  @EnableRedisRepositories(basePackageClasses = VerificationTokenRedisRepository.class)
  static class RedisRepoConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }
}
