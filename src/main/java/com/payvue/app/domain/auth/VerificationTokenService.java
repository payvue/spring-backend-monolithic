package com.payvue.app.domain.auth;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final VerificationProperties verificationProperties;

    public VerificationToken generateToken(User user, VerificationPurpose purpose) {
        UserAccountView view = UserAccountView.from(user);

        String identifier = purpose.getIdentifier(view);
        Map<String, String> metadata = purpose.buildMetadata(view, user);

        tokenRepository.delete(identifier, purpose);

        VerificationToken token = VerificationToken.generate(purpose, identifier, metadata);
        tokenRepository.save(token);

        return token;
    }

    public String generateVerificationUrl(VerificationToken token) {
        String baseUrl = verificationProperties.getBaseUrl();
        String endpoint = verificationProperties.getEndpoint();

        return String.format("%s%s?token=%s&purpose=%s", 
                baseUrl, 
                endpoint, 
                token.getToken(),
                token.getPurpose().name());
    }

    public VerificationToken verifyToken(String tokenString, VerificationPurpose purpose) {
        return tokenRepository.findByToken(tokenString, purpose)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
    }

    public VerificationToken verifyAndConsumeToken(String tokenString, VerificationPurpose purpose) {
        VerificationToken token = verifyToken(tokenString, purpose);
        deleteToken(token.getIdentifier(), purpose);
        return token;
    }

    public Optional<VerificationToken> findByIdentifier(String identifier, VerificationPurpose purpose) {
        return tokenRepository.findByIdentifier(identifier, purpose);
    }

    public boolean hasValidToken(String identifier, VerificationPurpose purpose) {
        return tokenRepository.exists(identifier, purpose);
    }

    public void deleteToken(String identifier, VerificationPurpose purpose) {
        tokenRepository.delete(identifier, purpose);
    }
}
