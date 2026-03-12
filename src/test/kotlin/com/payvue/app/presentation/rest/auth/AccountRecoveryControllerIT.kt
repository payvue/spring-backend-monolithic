package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.AuthUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.hamcrest.Matchers.nullValue
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest(classes = [com.payvue.PayvueApplication::class, AccountRecoveryControllerIT.TestSecurityBeans::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRecoveryControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var authUseCase: AuthUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("계정 복구 컨트롤러 통합 테스트") {
      context("아이디 찾기 요청에서") {
        it("성공 응답이 반환된다") {
          val payload = """{"email":"find-it@payvue.com"}"""

          mockMvc.post("/api/auth/find-id") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("가입하신 이메일로 아이디 정보가 전송되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }
        }
      }

      context("비밀번호 찾기 요청에서") {
        it("성공 응답이 반환된다") {
          val payload = """{"username":"login","email":"reset-it@payvue.com"}"""

          mockMvc.post("/api/auth/reset-password") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("비밀번호 재설정 링크가 이메일로 전송되었습니다.") }
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
