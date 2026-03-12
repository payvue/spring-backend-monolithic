package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class BearerTokenResolverTest {

  private final BearerTokenResolver resolver = new BearerTokenResolver();

  @Test
  @DisplayName("Bearer 토큰이 없으면 null을 반환한다")
  void returnsNullWhenMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  @DisplayName("Bearer prefix가 있으면 토큰을 반환한다")
  void returnsTokenWhenBearerHeaderPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token-123");

    assertThat(resolver.resolve(request)).isEqualTo("token-123");
  }

  @Test
  @DisplayName("Bearer prefix가 아니면 null을 반환한다")
  void returnsNullWhenNonBearerHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Basic token-123");

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  @DisplayName("요청이 null이면 null을 반환한다")
  void returnsNullWhenRequestNull() {
    assertThat(resolver.resolve(null)).isNull();
  }
}
