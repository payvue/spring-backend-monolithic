package com.payvue.app.domain.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VerificationToken {
    private final String token;
    private final VerificationPurpose purpose;
    private final String identifier;
    private final Instant createdAt;
    private final Map<String, String> metadata;

    private VerificationToken(
            String token,
            VerificationPurpose purpose,
            String identifier,
            Instant createdAt,
            Map<String, String> metadata
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("인증 목적은 필수입니다.");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("식별자는 필수입니다.");
        }

        this.token = token;
        this.purpose = purpose;
        this.identifier = identifier;
        this.createdAt = createdAt;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static VerificationToken generate(
            VerificationPurpose purpose,
            String identifier,
            Map<String, String> metadata
    ) {
        return new VerificationToken(
                UUID.randomUUID().toString(),
                purpose,
                identifier,
                Instant.now(),
                metadata
        );
    }

    public static VerificationToken reconstitute(
            String token,
            VerificationPurpose purpose,
            String identifier,
            Instant createdAt,
            Map<String, String> metadata
    ) {
        return new VerificationToken(token, purpose, identifier, createdAt, metadata);
    }

    public String getToken() {
        return token;
    }

    public VerificationPurpose getPurpose() {
        return purpose;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public String getMetadata(String key) {
        return metadata.get(key);
    }

    public long getTtlSeconds() {
        return purpose.getDefaultTtlSeconds();
    }
}
