package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthenticationFilterTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("유효한 토큰이면 SecurityContext에 인증을 설정한다")
  void setsAuthenticationWhenTokenValid() throws Exception {
    TokenProvider provider = mock(TokenProvider.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    SessionTokenRepository sessionTokenRepository = mock(SessionTokenRepository.class);
    TokenHeaderResolver resolver = mock(TokenHeaderResolver.class);

    when(resolver.resolve(any())).thenReturn("token");
    when(provider.validate("token")).thenReturn(true);
    when(sessionTokenRepository.isActive("token")).thenReturn(true);
    when(provider.getSubject("token")).thenReturn("a@b.com");
    when(userDetailsService.loadUserByUsername("a@b.com"))
        .thenReturn(new User("a@b.com", "pw", List.of(new SimpleGrantedAuthority("USER"))));

    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, userDetailsService, sessionTokenRepository, resolver);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getName()).isEqualTo("a@b.com");
  }

  @Test
  @DisplayName("토큰이 없거나 유효하지 않으면 인증을 설정하지 않는다")
  void skipsAuthenticationWhenTokenInvalid() throws Exception {
    TokenProvider provider = mock(TokenProvider.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    SessionTokenRepository sessionTokenRepository = mock(SessionTokenRepository.class);
    TokenHeaderResolver resolver = mock(TokenHeaderResolver.class);

    when(resolver.resolve(any())).thenReturn("token");
    when(provider.validate("token")).thenReturn(false);

    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, userDetailsService, sessionTokenRepository, resolver);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(userDetailsService, sessionTokenRepository);
  }

  @Test
  @DisplayName("토큰이 유효하지만 세션이 비활성 상태면 인증을 설정하지 않는다")
  void skipsAuthenticationWhenSessionInactive() throws Exception {
    TokenProvider provider = mock(TokenProvider.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    SessionTokenRepository sessionTokenRepository = mock(SessionTokenRepository.class);
    TokenHeaderResolver resolver = mock(TokenHeaderResolver.class);

    when(resolver.resolve(any())).thenReturn("token");
    when(provider.validate("token")).thenReturn(true);
    when(sessionTokenRepository.isActive("token")).thenReturn(false);

    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, userDetailsService, sessionTokenRepository, resolver);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(userDetailsService);
  }
}
