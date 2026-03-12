package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.auth.AuthCompletion;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.AuthSession;
import com.payvue.app.domain.auth.DirectCompletionResult;
import com.payvue.app.domain.auth.SessionManager;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.reactive.function.client.WebClient;

class OAuth2AuthenticationSuccessHandlerTest {

  @Test
  @DisplayName("OAuth2 인증 성공 시 Authorization 헤더와 HTML 응답을 반환한다")
  void respondsWithAuthHeaderAndHtml() throws Exception {
    AuthContextManager authContextManager = mock(AuthContextManager.class);
    AuthResult result = AuthResult.success(null)
        .withMeta(SessionManager.SESSION_META_KEY, new AuthSession("access-token", "refresh-token"));
    AuthCompletion completion = new DirectCompletionResult(result);
    when(authContextManager.authenticate(any(), any(), any(), any(AuthRequest.class)))
        .thenReturn(completion);

    UserContextManager userContextManager = mock(UserContextManager.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
    WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
    when(userContextManager.findByEmail(any(), any()))
        .thenReturn(Optional.of(mock(User.class)));
    when(webClientBuilder.build()).thenReturn(mock(WebClient.class));

    OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
        authContextManager,
        userContextManager,
        passwordEncoder,
        authorizedClientService,
        webClientBuilder
    );

    DefaultOAuth2User user = new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("USER")),
        Map.of("email", "user@payvue.com"),
        "email"
    );
    OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, token);

    assertThat(response.getRedirectedUrl()).contains("token=access-token");
    assertThat(response.getRedirectedUrl()).contains("provider=google");
  }

  @Test
  @DisplayName("이메일이 없어도 응답을 정상 반환한다")
  void respondsWithoutEmailAttribute() throws Exception {
    AuthContextManager authContextManager = mock(AuthContextManager.class);
    AuthResult result = AuthResult.success(null)
        .withMeta(SessionManager.SESSION_META_KEY, new AuthSession("access-token", "refresh-token"));
    AuthCompletion completion = new DirectCompletionResult(result);
    when(authContextManager.authenticate(any(), any(), any(), any(AuthRequest.class)))
        .thenReturn(completion);

    UserContextManager userContextManager = mock(UserContextManager.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
    WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
    when(userContextManager.findByEmail(any(), any()))
        .thenReturn(Optional.of(mock(User.class)));
    when(webClientBuilder.build()).thenReturn(mock(WebClient.class));

    OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
        authContextManager,
        userContextManager,
        passwordEncoder,
        authorizedClientService,
        webClientBuilder
    );

    java.util.Map<String, Object> attributes = new java.util.HashMap<>();
    attributes.put("email", null);
    DefaultOAuth2User user = new DefaultOAuth2User(
        List.of(new SimpleGrantedAuthority("USER")),
        attributes,
        "email"
    );
    OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, user.getAuthorities(), "github");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, token);

    assertThat(response.getRedirectedUrl()).contains("token=access-token");
    assertThat(response.getRedirectedUrl()).contains("provider=github");
  }
}
