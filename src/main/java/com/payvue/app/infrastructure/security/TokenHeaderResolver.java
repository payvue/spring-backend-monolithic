package com.payvue.app.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;

public interface TokenHeaderResolver {

  String resolve(HttpServletRequest request);
}
