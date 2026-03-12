package com.payvue.app.infrastructure.security;

import com.payvue.app.domain.auth.AuthCompletion;
import com.payvue.app.domain.auth.AuthContextManager;
import com.payvue.app.domain.auth.AuthRequest;
import com.payvue.app.domain.auth.AuthResult;
import com.payvue.app.domain.auth.AuthSession;
import com.payvue.app.domain.auth.CompletionStrategyClassification;
import com.payvue.app.domain.auth.SessionManager;
import com.payvue.app.domain.account.CommandType;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserPasswordGuard;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;

@Component
class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
  private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
  private static final String FRONTEND_ORIGIN = "http://localhost:3000";
  private static final String FRONTEND_CALLBACK_PATH = "/oauth/callback";
  private static final String OAUTH_PLACEHOLDER = "N/A";
  private static final String GITHUB_PROVIDER = "github";

  private final AuthContextManager authContextManager;
  private final UserContextManager userContextManager;
  private final PasswordEncoder passwordEncoder;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final WebClient webClient;

  OAuth2AuthenticationSuccessHandler(
      AuthContextManager authContextManager,
      UserContextManager userContextManager,
      PasswordEncoder passwordEncoder,
      OAuth2AuthorizedClientService authorizedClientService,
      WebClient.Builder webClientBuilder
  ) {
    this.authContextManager = authContextManager;
    this.userContextManager = userContextManager;
    this.passwordEncoder = passwordEncoder;
    this.authorizedClientService = authorizedClientService;
    this.webClient = webClientBuilder.build();
  }

  @Override
  public void onAuthenticationSuccess(
      jakarta.servlet.http.HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
    OAuth2User oauthUser = token.getPrincipal();

    String provider = token.getAuthorizedClientRegistrationId();
    java.util.Map<String, Object> attributes = oauthUser.getAttributes();
    Object rawProviderId = null;
    if (attributes != null) {
      rawProviderId = attributes.get("sub");
      if (rawProviderId == null) {
        rawProviderId = attributes.get("id");
      }
      if (rawProviderId == null) {
        rawProviderId = attributes.get("uid");
      }
      if (rawProviderId == null) {
        rawProviderId = attributes.get("email");
      }
    }
    String providerId = rawProviderId != null
        ? rawProviderId.toString()
        : (provider == null || provider.isBlank() ? "unknown" : provider + "-unknown");
    String email = Optional.ofNullable(oauthUser.getAttribute("email"))
        .map(Object::toString)
        .orElse(null);

    if (email == null || email.isBlank()) {
      email = buildFallbackEmail(provider, providerId);
    }

    if (email != null && !email.isBlank()) {
      ensureOAuthUser(email, provider, providerId);
    }

    AuthCompletion completion = authContextManager.authenticate(
        com.payvue.app.domain.auth.Authentication.OAUTH2,
        CompletionStrategyClassification.OAUTH_DYNAMIC,
        CompletionStrategyClassification.Sequence.VERIFIFY,
        AuthRequest.ofOAuth(provider, providerId, email)
    );

    AuthResult result = completion.result();

    AuthSession session = Optional.ofNullable(result.meta().get(SessionManager.SESSION_META_KEY))
        .filter(AuthSession.class::isInstance)
        .map(AuthSession.class::cast)
        .orElse(null);
    if (session == null) {
      response.setStatus(500);
      response.getWriter().write("OAuth 인증 세션 발급 실패");
      return;
    }

    String redirectUrl = FRONTEND_ORIGIN + FRONTEND_CALLBACK_PATH
        + "?token=" + session.accessToken()
        + "&provider=" + provider;
    response.sendRedirect(redirectUrl);
  }

  private void ensureOAuthUser(String email, String provider, String providerId) {
    if (userContextManager.findByEmail(email, User.class).isPresent()) {
      return;
    }
    String loginId = "oauth-" + (provider == null ? "unknown" : provider) + "-" + providerId;
    String rawPassword = "payvuePassw@rd!!";
    String encodedPassword = passwordEncoder.encode(rawPassword);
    String nickname = buildNickname(provider, providerId);
    User user = User.create(
        loginId,
        email,
        OAUTH_PLACEHOLDER,
        nickname,
        OAUTH_PLACEHOLDER,
        LocalDate.of(1970, 1, 1),
        OAUTH_PLACEHOLDER,
        OAUTH_PLACEHOLDER,
        OAUTH_PLACEHOLDER,
        OAUTH_PLACEHOLDER,
        OAUTH_PLACEHOLDER,
        encodedPassword,
        UserPasswordGuard.defaultPolicy(rawPassword)
    );
    user.activate();
    userContextManager.save(user, CommandType.REGISTER::name);
  }

  private String buildNickname(String provider, String providerId) {
    String prefix = "oauth-";
    String base = (provider == null || provider.isBlank()) ? "user" : provider;
    String suffix = providerId == null ? "" : "-" + providerId;
    String nickname = prefix + base + suffix;
    if (nickname.length() > 30) {
      return nickname.substring(0, 30);
    }
    return nickname;
  }

  private String buildFallbackEmail(String provider, String providerId) {
    String normalizedProvider = (provider == null || provider.isBlank()) ? "oauth" : provider;
    String normalizedId = providerId == null ? "unknown" : providerId.replaceAll("\\s+", "");
    return normalizedProvider + "-" + normalizedId + "@oauth.local";
  }

}
