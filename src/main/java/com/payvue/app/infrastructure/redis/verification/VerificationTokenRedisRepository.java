package com.payvue.app.infrastructure.redis.verification;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data Redis Repository
 */
@Repository
public interface VerificationTokenRedisRepository extends CrudRepository<VerificationTokenEntity, String> {

    /**
     * 토큰으로 조회 (역색인)
     */
    Optional<VerificationTokenEntity> findByToken(String token);
}
