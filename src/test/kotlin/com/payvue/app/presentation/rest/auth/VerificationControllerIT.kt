package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.VerificationUseCase
import com.payvue.app.application.auth.recovery.FindIdResult
import com.payvue.app.application.auth.recovery.ResetPasswordVerifyResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.auth.VerificationPurpose
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.hamcrest.Matchers.nullValue
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest(classes = [com.payvue.PayvueApplication::class, VerificationControllerIT.TestSecurityBeans::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificationControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var verificationUseCase: VerificationUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("인증 컨트롤러 통합 테스트") {
      context("아이디 찾기 인증 요청에서") {
        it("아이디 정보가 반환된다") {
          `when`(verificationUseCase.verifyFindId("token-it"))
            .thenReturn(FindIdResult("login", "masked@payvue.com"))

          mockMvc.get("/api/auth/verify") {
            param("token", "token-it")
            param("purpose", VerificationPurpose.FIND_ID.name)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("아이디를 찾았습니다.") }
            jsonPath("$.data.loginId") { value("login") }
            jsonPath("$.data.maskedEmail") { value("masked@payvue.com") }
          }
        }
      }

      context("인증 이메일 재발송 요청에서") {
        it("성공 응답이 반환된다") {
          mockMvc.post("/api/auth/resend-verification") {
            param("email", "resend-it@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("인증 이메일이 재발송되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }
        }
      }
    }
  }

  @TestConfiguration
  open class TestSecurityBeans {
    @Bean
    @Primary
    open fun tokenProvider(): TokenProvider {
      return object : TokenProvider {
        override fun generate(
          authenticatedPrincipal: com.payvue.supporting.user.command.AuthenticatedPrincipal,
          type: String
        ): String {
          return authenticatedPrincipal.email()
        }

        override fun validate(token: String): Boolean {
          return token.isNotBlank()
        }

        override fun getSubject(token: String): String {
          return token
        }

        override fun getClaims(token: String): Map<String, Any> {
          return mapOf("sub" to token)
        }
      }
    }

    @Bean
    @Primary
    open fun sessionTokenRepository(): SessionTokenRepository {
      return InMemorySessionTokenRepository()
    }
  }

  class InMemorySessionTokenRepository : SessionTokenRepository {
    private val tokens = ConcurrentHashMap<String, String>()

    override fun save(token: String, subject: String, ttl: Duration) {
      tokens[token] = subject
    }

    override fun isActive(token: String): Boolean {
      return tokens.containsKey(token)
    }

    override fun remove(token: String) {
      tokens.remove(token)
    }

    override fun delete(user: com.payvue.supporting.user.command.User) {
      tokens.entries.removeIf { it.value == UserAccountView.from(user).email() }
    }

    override fun delete(view: com.payvue.supporting.user.command.UserAccountView) {
      tokens.entries.removeIf { it.value == view.email() }
    }
  }
}
