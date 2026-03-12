package com.payvue.app.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.supporting.user.UserContextManager
import org.mockito.Mockito
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
open class SecurityTestConfiguration {

  @Bean
  internal open fun authContextManager(): AuthContextManager {
    return Mockito.mock(AuthContextManager::class.java)
  }

  @Bean
  internal open fun userContextManager(): UserContextManager {
    return Mockito.mock(UserContextManager::class.java)
  }

  @Bean
  internal open fun passwordEncoder(): PasswordEncoder {
    return Mockito.mock(PasswordEncoder::class.java)
  }

  @Bean
  internal open fun authorizedClientService(): OAuth2AuthorizedClientService {
    return Mockito.mock(OAuth2AuthorizedClientService::class.java)
  }

  @Bean
  internal open fun webClientBuilder(): WebClient.Builder {
    return WebClient.builder()
  }

  @Bean
  internal open fun oauth2AuthenticationSuccessHandler(
    authContextManager: AuthContextManager,
    userContextManager: UserContextManager,
    passwordEncoder: PasswordEncoder,
    authorizedClientService: OAuth2AuthorizedClientService,
    webClientBuilder: WebClient.Builder
  ): OAuth2AuthenticationSuccessHandler {
    return OAuth2AuthenticationSuccessHandler(
      authContextManager,
      userContextManager,
      passwordEncoder,
      authorizedClientService,
      webClientBuilder
    )
  }

  @Bean
  internal open fun restAccessDeniedHandler(objectMapper: ObjectMapper): RestAccessDeniedHandler {
    return RestAccessDeniedHandler(objectMapper)
  }

  @Bean
  internal open fun restAuthenticationEntryPoint(objectMapper: ObjectMapper): RestAuthenticationEntryPoint {
    return RestAuthenticationEntryPoint(objectMapper)
  }
}
