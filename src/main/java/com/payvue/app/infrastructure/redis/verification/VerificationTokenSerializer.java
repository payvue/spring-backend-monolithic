package com.payvue.app.infrastructure.redis.verification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.domain.auth.VerificationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * VerificationToken 직렬화/역직렬화 컴포넌트
 * Redis 저장을 위한 JSON 변환
 */
@Component
public class VerificationTokenSerializer {

    private final ObjectMapper objectMapper;

    public VerificationTokenSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * VerificationToken → JSON 문자열
     */
    public String serialize(VerificationToken token) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("token", token.getToken());
            data.put("purpose", token.getPurpose().name());
            data.put("identifier", token.getIdentifier());
            data.put("createdAt", token.getCreatedAt().getEpochSecond());
            data.put("metadata", token.getMetadata());

            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("토큰 직렬화 실패", e);
        }
    }

    /**
     * JSON 문자열 → VerificationToken
     */
    @SuppressWarnings("unchecked")
    public VerificationToken deserialize(
            String json,
            VerificationPurpose purpose,
            String identifier
    ) {
        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            String token = (String) data.get("token");
            long createdAtEpoch = ((Number) data.get("createdAt")).longValue();
            Instant createdAt = Instant.ofEpochSecond(createdAtEpoch);
            Map<String, String> metadata = (Map<String, String>) data.getOrDefault("metadata", new HashMap<>());

            return VerificationToken.reconstitute(token, purpose, identifier, createdAt, metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("토큰 역직렬화 실패", e);
        }
    }
}
