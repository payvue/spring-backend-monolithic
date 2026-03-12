package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.security.SessionTokenRepository;
import com.payvue.app.domain.security.TokenProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@SpringBootTest(classes = SecurityConfigurationBootTest.TestApplication.class)
class SecurityConfigurationBootTest {

  @TestConfiguration
  @EnableAutoConfiguration
  @Import(SecurityConfiguration.class)
  static class TestApplication {}

  @Autowired
  private FilterChainProxy filterChainProxy;

  @MockBean
  private TokenProvider tokenProvider;

  @MockBean
  private SessionTokenRepository sessionTokenRepository;

  @MockBean
  private UserDetailsService userDetailsService;

  @MockBean
  private RestAccessDeniedHandler restAccessDeniedHandler;

  @MockBean
  private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

  @MockBean
  private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

  private MockHttpServletRequest swaggerRequest;
  private MockHttpServletRequest oauthRequest;
  private MockHttpServletRequest apiRequest;
  private MockHttpServletRequest apiPostRequest;

  @BeforeEach
  void setupRequests() {
    swaggerRequest = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    oauthRequest = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
    apiRequest = new MockHttpServletRequest("GET", "/api/user/me");
    apiPostRequest = new MockHttpServletRequest("POST", "/api/user/me");
  }

  @Test
  @DisplayName("Swagger 경로는 JWT 관련 컴포넌트를 호출하지 않는다")
  void swaggerChainSkipsJwtFlow() throws Exception {
    filterChainProxy.doFilter(swaggerRequest, new MockHttpServletResponse(), new MockFilterChain());

    verifyNoInteractions(tokenProvider, sessionTokenRepository, userDetailsService);
  }

  @Test
  @DisplayName("OAuth 경로는 JWT 관련 컴포넌트를 호출하지 않는다")
  void oauthChainSkipsJwtFlow() throws Exception {
    filterChainProxy.doFilter(oauthRequest, new MockHttpServletResponse(), new MockFilterChain());

    verifyNoInteractions(tokenProvider, sessionTokenRepository, userDetailsService);
  }

  @Test
  @DisplayName("API 경로는 JWT 검증 흐름을 수행한다")
  void apiChainInvokesJwtFlow() throws Exception {
    apiRequest.addHeader("Authorization", "Bearer token");
    when(tokenProvider.validate("token")).thenReturn(true);
    when(sessionTokenRepository.isActive("token")).thenReturn(true);
    when(tokenProvider.getSubject("token")).thenReturn("user@payvue.com");
    when(userDetailsService.loadUserByUsername("user@payvue.com"))
        .thenReturn(new User("user@payvue.com", "pw", List.of(new SimpleGrantedAuthority("USER"))));

    filterChainProxy.doFilter(apiRequest, new MockHttpServletResponse(), new MockFilterChain());

    verify(tokenProvider).validate("token");
    verify(sessionTokenRepository).isActive("token");
    verify(userDetailsService).loadUserByUsername("user@payvue.com");
  }


  @Test
  @DisplayName("CSRF가 비활성화되어 POST 요청도 인증 진입점이 호출된다")
  void csrfDisabledAllowsAuthenticationEntryPoint() throws Exception {
    apiPostRequest.addHeader("Authorization", "Bearer token");
    when(tokenProvider.validate("token")).thenReturn(false);

    filterChainProxy.doFilter(apiPostRequest, new MockHttpServletResponse(), new MockFilterChain());

    verify(restAuthenticationEntryPoint)
        .commence(any(), any(), any());
    verifyNoInteractions(restAccessDeniedHandler);
  }

  @Test
  @DisplayName("세션 정책이 STATELESS라서 요청 후 세션이 생성되지 않는다")
  void statelessSessionPolicyDoesNotCreateSession() throws Exception {
    apiRequest.addHeader("Authorization", "Bearer token");
    when(tokenProvider.validate("token")).thenReturn(true);
    when(sessionTokenRepository.isActive("token")).thenReturn(true);
    when(tokenProvider.getSubject("token")).thenReturn("user@payvue.com");
    when(userDetailsService.loadUserByUsername("user@payvue.com"))
        .thenReturn(new User("user@payvue.com", "pw", List.of(new SimpleGrantedAuthority("USER"))));

    filterChainProxy.doFilter(apiRequest, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(apiRequest.getSession(false)).isNull();
  }
}
