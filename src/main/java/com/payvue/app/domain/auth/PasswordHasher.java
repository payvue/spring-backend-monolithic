package com.payvue.app.domain.auth;

import com.payvue.app.domain.security.PasswordHash;

interface PasswordHasher {
  boolean matches(String rawPassword, PasswordHash hash);
}
