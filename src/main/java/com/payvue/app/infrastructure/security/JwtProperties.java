package com.payvue.app.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
class JwtProperties {
  private String issuer;
  private Duration accessTokenTtl;
  private Duration refreshTokenTtl;
  private String secret;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public Duration getAccessTokenTtl() {
    return accessTokenTtl;
  }

  public void setAccessTokenTtl(Duration accessTokenTtl) {
    this.accessTokenTtl = accessTokenTtl;
  }

  public Duration getRefreshTokenTtl() {
    return refreshTokenTtl;
  }

  public void setRefreshTokenTtl(Duration refreshTokenTtl) {
    this.refreshTokenTtl = refreshTokenTtl;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
}
