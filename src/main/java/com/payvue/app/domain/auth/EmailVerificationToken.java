package com.payvue.app.domain.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@EqualsAndHashCode
class EmailVerificationToken {
    private final String token;
    private final String email;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean used;

    private EmailVerificationToken(
            String token, 
            String email, 
            Instant createdAt, 
            Instant expiresAt
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("만료 시간은 생성 시간 이후여야 합니다.");
        }
        
        this.token = token;
        this.email = email;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public static EmailVerificationToken generate(String email, int hoursToExpire) {
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(hoursToExpire * 3600L);
        
        return new EmailVerificationToken(token, email, now, expires);
    }

    public static EmailVerificationToken reconstitute(
            String token,
            String email,
            Instant createdAt,
            Instant expiresAt,
            boolean used
    ) {
        EmailVerificationToken tokenObj = new EmailVerificationToken(
                token, email, createdAt, expiresAt
        );
        tokenObj.used = used;
        return tokenObj;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        if (used) {
            throw new IllegalStateException("이미 사용된 토큰입니다.");
        }
        if (isExpired()) {
            throw new IllegalStateException("만료된 토큰입니다.");
        }
        this.used = true;
    }
}
