package com.payvue.app.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
class BearerTokenResolver {

  public String resolve(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
