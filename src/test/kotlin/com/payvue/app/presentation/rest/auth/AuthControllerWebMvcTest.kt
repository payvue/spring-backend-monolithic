package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.AuthUseCase
import com.payvue.app.application.auth.login.LoginResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import com.payvue.app.infrastructure.security.TokenHeaderResolver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class AuthControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var authUseCase: AuthUseCase

  @MockBean
  private lateinit var tokenHeaderResolver: TokenHeaderResolver

  @MockBean
  private lateinit var tokenProvider: TokenProvider

  @MockBean
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var userDetailsService: UserDetailsService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    beforeTest {
      setupAuthMocks("tester@payvue.com")
    }

    describe("로그인 요청") {
      context("유효한 요청이 들어오면") {
        it("access 토큰이 포함된 응답이 반환된다") {
          `when`(authUseCase.login(any())).thenReturn(LoginResult("access-token", "refresh-token"))

          val payload = """
            {"email":"login@payvue.com","password":"Passw0rd!"}
          """.trimIndent()

          mockMvc.perform(
            post("/user/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청하신 응답이 완료되었습니다."))
            .andExpect(jsonPath("$.data.access").value("access-token"))
        }
      }
    }

    describe("로그아웃 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          mockMvc.perform(
            post("/user/auth/logout")
              .contentType(MediaType.APPLICATION_JSON)
          )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
        }
      }

      context("인증된 요청이면") {
        it("성공 응답이 반환된다") {
          `when`(tokenHeaderResolver.resolve(any())).thenReturn("access-token")

          mockMvc.perform(
            post("/user/auth/logout")
              .header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("요청하신 응답이 완료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

          verify(authUseCase).logout(any(UserDetails::class.java), any())
        }
      }
    }
  }

  private fun setupAuthMocks(email: String) {
    val userDetails = Mockito.mock(UserDetails::class.java)
    `when`(userDetails.username).thenReturn(email)
    `when`(tokenProvider.validate(anyString())).thenReturn(true)
    `when`(tokenProvider.getSubject(anyString())).thenAnswer { it.arguments[0] as String }
    `when`(sessionTokenRepository.isActive(anyString())).thenReturn(true)
    `when`(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails)
  }
}
