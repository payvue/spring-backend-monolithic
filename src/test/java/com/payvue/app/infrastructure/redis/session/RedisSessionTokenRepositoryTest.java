package com.payvue.app.infrastructure.redis.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.infrastructure.redis.RedisContainerSupport;
import com.payvue.supporting.user.command.UserAccountView;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(classes = {RedisAutoConfiguration.class, RedisSessionTokenRepository.class})
class RedisSessionTokenRepositoryTest extends RedisContainerSupport {

  @Autowired
  private RedisSessionTokenRepository repository;

  @Autowired
  private StringRedisTemplate template;

  @Test
  @DisplayName("토큰을 저장하면 access key와 사용자 인덱스가 생성된다")
  void saveStoresTokenAndIndex() {
    repository.save("token-1", "user@payvue.com", Duration.ofSeconds(60));

    assertThat(template.hasKey(accessKey("token-1"))).isTrue();
    assertThat(template.opsForSet().isMember(userKey("user@payvue.com"), "token-1")).isTrue();
    assertThat(template.getExpire(userKey("user@payvue.com"))).isPositive();
  }

  @Test
  @DisplayName("토큰을 제거하면 관련 키가 삭제된다")
  void removeDeletesKeys() {
    repository.save("token-2", "user@payvue.com", Duration.ofSeconds(60));

    repository.remove("token-2");

    assertThat(template.hasKey(accessKey("token-2"))).isFalse();
    assertThat(template.opsForSet().isMember(userKey("user@payvue.com"), "token-2")).isFalse();
  }

  @Test
  @DisplayName("UserAccountView로 삭제하면 토큰과 인덱스가 삭제된다")
  void deleteByUserAccountViewRemovesTokens() {
    repository.save("token-3", "user@payvue.com", Duration.ofSeconds(60));
    repository.save("token-4", "user@payvue.com", Duration.ofSeconds(60));

    UserAccountView view = new UserAccountView(
        null,
        null,
        "user@payvue.com",
        null,
        false,
        null,
        null,
        null,
        false,
        null,
        Map.of()
    );

    repository.delete(view);

    assertThat(template.hasKey(accessKey("token-3"))).isFalse();
    assertThat(template.hasKey(accessKey("token-4"))).isFalse();
    assertThat(template.hasKey(userKey("user@payvue.com"))).isFalse();
  }

  @Test
  @DisplayName("isActive는 저장된 토큰이면 true를 반환한다")
  void isActiveReturnsTrueWhenTokenExists() {
    repository.save("token-5", "user@payvue.com", Duration.ofSeconds(60));

    assertThat(repository.isActive("token-5")).isTrue();
  }

  @Test
  @DisplayName("isActive는 토큰이 없으면 false를 반환한다")
  void isActiveReturnsFalseWhenTokenMissing() {
    assertThat(repository.isActive("missing")).isFalse();
  }

  private String accessKey(String token) {
    return "auth:access:" + token;
  }

  private String userKey(String subject) {
    return "auth:access:user:" + subject;
  }
}
