package com.payvue.app.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.convert.RedisConverter;
import org.springframework.data.redis.core.mapping.RedisMappingContext;


@Configuration
@Import(RedisAutoConfiguration.class)
public class RedisRepositoryConfiguration {

  @Bean
  public RedisKeyValueAdapter redisKeyValueAdapter(
      RedisOperations<Object, Object> redisOperations
  ) {
    RedisMappingContext mappingContext = new RedisMappingContext();
    MappingRedisConverter redisConverter = new MappingRedisConverter(mappingContext);
    redisConverter.afterPropertiesSet();
    return new DirectKeyRedisKeyValueAdapter(redisOperations, redisConverter);
  }

  private static final class DirectKeyRedisKeyValueAdapter extends RedisKeyValueAdapter {

    private DirectKeyRedisKeyValueAdapter(
        RedisOperations<?, ?> redisOperations,
        RedisConverter redisConverter
    ) {
      super(redisOperations, redisConverter);
    }

    @Override
    public byte[] createKey(String keyspace, String id) {
      return toBytes(id);
    }
  }
}
