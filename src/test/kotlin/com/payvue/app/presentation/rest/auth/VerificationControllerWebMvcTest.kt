package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.VerificationUseCase
import com.payvue.app.application.auth.recovery.FindIdResult
import com.payvue.app.application.auth.recovery.ResetPasswordVerifyResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.auth.VerificationPurpose
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(VerificationController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class VerificationControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var verificationUseCase: VerificationUseCase

  @MockBean
  private lateinit var tokenProvider: TokenProvider

  @MockBean
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var userDetailsService: UserDetailsService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("인증 토큰 검증 요청") {
      context("회원가입 인증이면") {
        it("성공 응답이 반환된다") {
          mockMvc.perform(
            get("/api/auth/verify")
              .param("token", "token-1")
              .param("purpose", VerificationPurpose.SIGNUP.name)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

          verify(verificationUseCase).verifySignup("token-1")
        }
      }

      context("아이디 찾기 인증이면") {
        it("아이디 정보가 반환된다") {
          `when`(verificationUseCase.verifyFindId("token-2"))
            .thenReturn(FindIdResult("login", "masked@payvue.com"))

          mockMvc.perform(
            get("/api/auth/verify")
              .param("token", "token-2")
              .param("purpose", VerificationPurpose.FIND_ID.name)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("아이디를 찾았습니다."))
            .andExpect(jsonPath("$.data.loginId").value("login"))
            .andExpect(jsonPath("$.data.maskedEmail").value("masked@payvue.com"))
        }
      }

      context("비밀번호 재설정 인증이면") {
        it("임시 비밀번호가 반환된다") {
          `when`(verificationUseCase.verifyResetPassword("token-3"))
            .thenReturn(ResetPasswordVerifyResult("temp-1234"))

          mockMvc.perform(
            get("/api/auth/verify")
              .param("token", "token-3")
              .param("purpose", VerificationPurpose.RESET_PASSWORD.name)
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("임시 비밀번호가 발급되었습니다."))
            .andExpect(jsonPath("$.data.temporaryPassword").value("temp-1234"))
        }
      }

      context("검증 실패가 발생하면") {
        it("INVALID_REQUEST 응답이 반환된다") {
          doThrow(IllegalArgumentException("invalid"))
            .`when`(verificationUseCase).verifySignup("token-4")

          mockMvc.perform(
            get("/api/auth/verify")
              .param("token", "token-4")
              .param("purpose", VerificationPurpose.SIGNUP.name)
          )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("invalid"))
            .andExpect(jsonPath("$.data").doesNotExist())
        }
      }
    }

    describe("인증 이메일 재발송 요청") {
      context("유효한 요청이 들어오면") {
        it("성공 응답이 반환된다") {
          mockMvc.perform(
            post("/api/auth/resend-verification")
              .param("email", "resend@payvue.com")
          )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("인증 이메일이 재발송되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

          verify(verificationUseCase).resendSignupVerification("resend@payvue.com")
        }
      }
    }
  }
}
