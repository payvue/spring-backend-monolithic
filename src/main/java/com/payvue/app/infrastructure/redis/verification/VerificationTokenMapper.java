package com.payvue.app.infrastructure.redis.verification;

import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import org.springframework.stereotype.Component;

/**
 * VerificationToken ↔ VerificationTokenEntity 변환
 */
@Component
public class VerificationTokenMapper {

    private final VerificationKeyGenerator keyGenerator;

    public VerificationTokenMapper(VerificationKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    /**
     * Domain → Entity
     */
    public VerificationTokenEntity toEntity(VerificationToken domain) {
        return VerificationTokenEntity.builder()
                .id(keyGenerator.generateKey(domain.getPurpose(), domain.getIdentifier()))
                .token(domain.getToken())
                .purpose(domain.getPurpose().name())
                .identifier(domain.getIdentifier())
                .createdAt(domain.getCreatedAt().getEpochSecond())
                .metadata(domain.getMetadata())
                .ttl(domain.getTtlSeconds())
                .build();
    }

    /**
     * Entity → Domain
     */
    public VerificationToken toDomain(VerificationTokenEntity entity) {
        return VerificationToken.reconstitute(
                entity.getToken(),
                entity.getPurposeEnum(),
                entity.getIdentifier(),
                entity.getCreatedAtInstant(),
                entity.getMetadata()
        );
    }
}
