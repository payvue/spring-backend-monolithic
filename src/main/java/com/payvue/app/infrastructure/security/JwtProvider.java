package com.payvue.app.infrastructure.security;

import com.payvue.app.domain.security.TokenProvider;
import com.payvue.supporting.user.command.AuthenticatedPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class JwtProvider implements TokenProvider {
  private final JwtProperties properties;
  private final Key key;
  private final Clock clock;

  JwtProvider(JwtProperties properties) {
    this.properties = properties;
    this.clock = Clock.systemUTC();
    this.key = buildKey(properties.getSecret());
  }

  @Override
  public String generate(AuthenticatedPrincipal authenticatedPrincipal, String type) {
    Duration ttl = resolveTtl(type);
    return createToken(authenticatedPrincipal, ttl, type);
  }

  @Override
  public boolean validate(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  @Override
  public String getSubject(String token) {
    return parseClaims(token).getSubject();
  }

  @Override
  public java.util.Map<String, Object> getClaims(String token) {
    return parseClaims(token);
  }

  private String createToken(AuthenticatedPrincipal authenticatedPrincipal, Duration ttl, String type) {
    Instant now = Instant.now(clock);
    return Jwts.builder()
        .setIssuer(properties.getIssuer())
        .setSubject(authenticatedPrincipal.email())
        .claim("type", type)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(ttl)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  private Duration resolveTtl(String type) {
    if (SessionTokenType.valueOf(type).equals(SessionTokenType.REFRESH)) {
      return properties.getRefreshTokenTtl();
    }
    return properties.getAccessTokenTtl();
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Key buildKey(String secret) {
    if (secret == null) {
      throw new IllegalArgumentException("JWT secret must be configured");
    }
    byte[] bytes = secret.length() >= 32
        ? secret.getBytes(StandardCharsets.UTF_8)
        : Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(bytes);
  }
}
