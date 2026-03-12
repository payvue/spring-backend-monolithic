package com.payvue.app.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisKeyValueAdapter;

@SpringBootTest(classes = RedisRepositoryConfiguration.class)
class RedisRepositoryConfigurationBootTest extends RedisContainerSupport {

  @Autowired
  private RedisKeyValueAdapter redisKeyValueAdapter;

  @Test
  @DisplayName("RedisRepositoryConfiguration은 직접 키 전략을 사용한다")
  void usesDirectKeyStrategy() {
    byte[] key = redisKeyValueAdapter.createKey("verification", "id-1");

    assertThat(new String(key)).isEqualTo("id-1");
  }
}
