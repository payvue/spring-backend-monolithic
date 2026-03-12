package com.payvue.app.domain.auth;

@FunctionalInterface
public interface AuthSequenceHandler {
  AuthResult handle(AuthRequest request, AuthContext context);
}
