package com.payvue.app.domain.security;

import com.payvue.supporting.user.command.AuthenticatedPrincipal;

public interface TokenProvider {
  String generate(AuthenticatedPrincipal authenticatedPrincipal, String type);
  boolean validate(String token);
  String getSubject(String token);
  java.util.Map<String, Object> getClaims(String token);
}
