package com.payvue.app.infrastructure.redis.verification;

import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import com.payvue.app.domain.auth.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * Redis 기반 인증 토큰 Repository 구현체
 * Spring Data Redis + @RedisHash 사용
 */
@Repository
@RequiredArgsConstructor
public class RedisVerificationTokenRepository implements VerificationTokenRepository {

    private final VerificationTokenRedisRepository redisRepository;
    private final VerificationTokenMapper mapper;
    private final VerificationKeyGenerator keyGenerator;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(VerificationToken token) {
        VerificationTokenEntity entity = mapper.toEntity(token);
        redisRepository.save(entity);
        if (token.getToken() == null) {
            return;
        }
        String indexKey = keyGenerator.generateTokenIndexKey(token.getToken());
        String indexValue = keyGenerator.generateIndexValue(token.getPurpose(), token.getIdentifier());
        long ttlSeconds = token.getTtlSeconds();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(indexKey, indexValue, ttlSeconds, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(indexKey, indexValue);
        }
    }

    @Override
    public Optional<VerificationToken> findByToken(String token, VerificationPurpose purpose) {
        String indexKey = keyGenerator.generateTokenIndexKey(token);
        String indexValue = redisTemplate.opsForValue().get(indexKey);
        if (indexValue == null) {
            return Optional.empty();
        }
        VerificationKeyGenerator.ParsedIndexValue parsed = keyGenerator.parseIndexValue(indexValue);
        if (parsed.purpose() != purpose) {
            return Optional.empty();
        }
        Optional<VerificationToken> found = findByIdentifier(parsed.identifier(), parsed.purpose());
        if (found.isPresent() && !token.equals(found.get().getToken())) {
            redisTemplate.delete(indexKey);
            return Optional.empty();
        }
        if (found.isEmpty()) {
            redisTemplate.delete(indexKey);
        }
        return found;
    }

    @Override
    public Optional<VerificationToken> findByIdentifier(String identifier, VerificationPurpose purpose) {
        String key = keyGenerator.generateKey(purpose, identifier);
        return redisRepository.findById(key)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(String identifier, VerificationPurpose purpose) {
        String key = keyGenerator.generateKey(purpose, identifier);
        redisRepository.findById(key)
                .map(VerificationTokenEntity::getToken)
                .filter(value -> !value.isBlank())
                .ifPresent(token -> redisTemplate.delete(
                        keyGenerator.generateTokenIndexKey(token)
                ));
        redisRepository.deleteById(key);
    }

    @Override
    public boolean exists(String identifier, VerificationPurpose purpose) {
        String key = keyGenerator.generateKey(purpose, identifier);
        return redisRepository.existsById(key);
    }
}
