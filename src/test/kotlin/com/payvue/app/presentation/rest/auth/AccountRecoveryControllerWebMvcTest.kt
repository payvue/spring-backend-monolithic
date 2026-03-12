package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.AuthUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(AccountRecoveryController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class AccountRecoveryControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var authUseCase: AuthUseCase

  @MockBean
  private lateinit var tokenProvider: TokenProvider

  @MockBean
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var userDetailsService: UserDetailsService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("아이디 찾기 요청") {
      context("유효한 요청이 들어오면") {
        it("성공 응답이 반환된다") {
          val payload = """{"email":"find@payvue.com"}"""

          mockMvc.perform(
            post("/api/auth/find-id")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("가입하신 이메일로 아이디 정보가 전송되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

          verify(authUseCase).findId(any())
        }
      }

      context("요청 처리 중 예외가 발생하면") {
        it("INVALID_REQUEST 응답이 반환된다") {
          doThrow(IllegalArgumentException("invalid"))
            .`when`(authUseCase).findId(any())

          val payload = """{"email":"find@payvue.com"}"""

          mockMvc.perform(
            post("/api/auth/find-id")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload)
          )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("invalid"))
            .andExpect(jsonPath("$.data").doesNotExist())
        }
      }
    }

    describe("비밀번호 찾기 요청") {
      context("유효한 요청이 들어오면") {
        it("성공 응답이 반환된다") {
          val payload = """{"username":"login","email":"reset@payvue.com"}"""

          mockMvc.perform(
            post("/api/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("비밀번호 재설정 링크가 이메일로 전송되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

          verify(authUseCase).findPassword(any())
        }
      }

      context("요청 처리 중 예외가 발생하면") {
        it("INVALID_REQUEST 응답이 반환된다") {
          doThrow(IllegalArgumentException("invalid"))
            .`when`(authUseCase).findPassword(any())

          val payload = """{"username":"login","email":"reset@payvue.com"}"""

          mockMvc.perform(
            post("/api/auth/reset-password")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload)
          )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("invalid"))
            .andExpect(jsonPath("$.data").doesNotExist())
        }
      }
    }
  }
}
