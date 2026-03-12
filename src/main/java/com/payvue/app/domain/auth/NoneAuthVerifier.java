package com.payvue.app.domain.auth;

import lombok.NoArgsConstructor;

@NoArgsConstructor
class NoneAuthVerifier implements AuthSequenceHandler {

    @Override
    public AuthResult handle(AuthRequest request, AuthContext context) {
        return AuthResult.failure(AuthErrorCode.UNSUPPORTED_AUTH);
    }
}
