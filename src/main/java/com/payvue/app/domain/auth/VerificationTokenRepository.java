package com.payvue.app.domain.auth;

import java.util.Optional;

public interface VerificationTokenRepository {

    void save(VerificationToken token);

    Optional<VerificationToken> findByToken(String token, VerificationPurpose purpose);

    Optional<VerificationToken> findByIdentifier(String identifier, VerificationPurpose purpose);

    void delete(String identifier, VerificationPurpose purpose);

    boolean exists(String identifier, VerificationPurpose purpose);
}
