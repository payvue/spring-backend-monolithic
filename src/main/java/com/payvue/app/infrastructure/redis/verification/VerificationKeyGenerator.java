package com.payvue.app.infrastructure.redis.verification;

import com.payvue.app.domain.auth.VerificationPurpose;
import org.springframework.stereotype.Component;

/**
 * Redis Key 생성 전략
 * 인프라 관심사를 도메인에서 분리
 */
@Component
public class VerificationKeyGenerator {

    private static final String KEY_PREFIX = "verification";
    private static final String TOKEN_INDEX_PREFIX = "verification:token";

    /**
     * 메인 키 생성
     * 패턴: verification:{purpose}:{identifier}
     */
    public String generateKey(VerificationPurpose purpose, String identifier) {
        return String.format("%s:%s:%s", KEY_PREFIX, getPurposeKey(purpose), identifier);
    }

    /**
     * 토큰 역색인 키 생성
     * 패턴: verification:token:{uuid}
     */
    public String generateTokenIndexKey(String token) {
        return String.format("%s:%s", TOKEN_INDEX_PREFIX, token);
    }

    /**
     * 역색인 값 생성
     * 패턴: {PURPOSE}:{identifier}
     */
    public String generateIndexValue(VerificationPurpose purpose, String identifier) {
        return String.format("%s:%s", purpose.name(), identifier);
    }

    /**
     * 역색인 값 파싱
     */
    public ParsedIndexValue parseIndexValue(String indexValue) {
        String[] parts = indexValue.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("잘못된 역색인 형식: " + indexValue);
        }

        return new ParsedIndexValue(
                VerificationPurpose.valueOf(parts[0]),
                parts[1]
        );
    }

    /**
     * Purpose에 따른 키 접두사
     */
    private String getPurposeKey(VerificationPurpose purpose) {
        return purpose.name();
    }

    /**
     * 파싱된 역색인 값
     */
    public record ParsedIndexValue(
            VerificationPurpose purpose,
            String identifier
    ) {}
}
