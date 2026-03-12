package com.payvue.app.infrastructure.redis.session;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.time.Duration;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionTokenRepository implements SessionTokenRepository {

  private static final String ACCESS_PREFIX = "auth:access:";
  private static final String USER_INDEX_PREFIX = "auth:access:user:";

  private final StringRedisTemplate template;

  public RedisSessionTokenRepository(StringRedisTemplate template) {
    this.template = template;
  }

  @Override
  public void save(String token, String subject, Duration ttl) {
    if (token == null || ttl == null) {
      return;
    }
    template.opsForValue().set(key(token), subject, ttl);
    if (subject != null && !subject.isBlank()) {
      template.opsForSet().add(userKey(subject), token);
      if (!ttl.isZero() && !ttl.isNegative()) {
        template.expire(userKey(subject), ttl);
      }
    }
  }

  @Override
  public boolean isActive(String token) {
    if (token == null) {
      return false;
    }
    Boolean exists = template.hasKey(key(token));
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public void remove(String token) {
    if (token == null) {
      return;
    }
    String subject = template.opsForValue().get(key(token));
    template.delete(key(token));
    if (subject != null && !subject.isBlank()) {
      template.opsForSet().remove(userKey(subject), token);
    }
  }

  @Override
  public void delete(User user) {
    if (user == null) {
      return;
    }
    String email = UserAccountView.from(user).email();
    if (email == null || email.isBlank()) {
      return;
    }
    String key = userKey(email);
    Set<String> tokens = template.opsForSet().members(key);
    if (tokens != null) {
      for (String token : tokens) {
        template.delete(key(token));
      }
    }
    template.delete(key);
  }

  @Override
  public void delete(UserAccountView view) {
    String email = view.email();
    if (email == null || email.isBlank()) {
      return;
    }
    String key = userKey(email);
    Set<String> tokens = template.opsForSet().members(key);
    if (tokens != null) {
      for (String token : tokens) {
        template.delete(key(token));
      }
    }
    template.delete(key);
  }

  private String key(String token) {
    return ACCESS_PREFIX + token;
  }

  private String userKey(String subject) {
    return USER_INDEX_PREFIX + subject;
  }
}
