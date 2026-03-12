package com.payvue.app.infrastructure.redis.verification;

import com.payvue.app.domain.auth.VerificationPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 저장용 VerificationToken Entity
 * 인프라 레이어 객체
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "verification")
public class VerificationTokenEntity {

    /**
     * Redis Key: verification:{purpose}:{identifier}
     */
    @Id
    private String id;

    /**
     * UUID 토큰 (역색인용)
     */
    private String token;

    /**
     * 인증 목적
     */
    private String purpose;

    /**
     * 식별자 (email 또는 phone)
     */
    private String identifier;

    /**
     * 생성 시간 (epoch seconds)
     */
    private Long createdAt;

    /**
     * 메타데이터 (JSON 직렬화)
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * TTL (초 단위)
     */
    @TimeToLive
    private Long ttl;

    /**
     * Purpose Enum으로 변환
     */
    public VerificationPurpose getPurposeEnum() {
        return VerificationPurpose.valueOf(purpose);
    }

    /**
     * 생성 시간 Instant로 변환
     */
    public Instant getCreatedAtInstant() {
        return Instant.ofEpochSecond(createdAt);
    }
}
