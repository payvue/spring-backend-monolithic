package com.payvue.app.presentation.rest.auth

import com.payvue.app.application.auth.AuthUseCase
import com.payvue.app.application.auth.login.LoginResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import org.hamcrest.Matchers.nullValue
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest(classes = [com.payvue.PayvueApplication::class, AuthControllerIT.TestSecurityBeans::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var authUseCase: AuthUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("인증 컨트롤러 통합 테스트") {
      context("로그인 요청에서") {
        it("정상 응답과 access 토큰이 반환된다") {
          createAndPersistUser("login-it@payvue.com")
          `when`(authUseCase.login(any())).thenReturn(LoginResult("access-token", "refresh-token"))

          val payload = """
            {"email":"login-it@payvue.com","Passw0rd!":"Passw0rd!"}
          """.trimIndent()

          mockMvc.post("/user/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.access") { value("access-token") }
          }
        }
      }

      context("로그아웃 요청에서") {
        it("인증된 요청이면 성공 응답이 반환된다") {
          val user = createAndPersistUser("logout-it@payvue.com")
          val token = UserAccountView.from(user).email()
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))

          mockMvc.post("/user/auth/logout") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }
        }
      }
    }
  }

  private fun createAndPersistUser(email: String): User {
    val user = User.create(
      "login",
      email,
      "010-9999-8888",
      "nickname",
      "real-name",
      LocalDate.of(1990, 1, 1),
      "11",
      "110",
      "road",
      "detail",
      "12345",
      "encoded",
      UserPasswordGuard.defaultPolicy("Passw0rd!")
    )
    return userRepository.save(user)
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

    override fun delete(user: User) {
      tokens.entries.removeIf { it.value == UserAccountView.from(user).email() }
    }

    override fun delete(view: com.payvue.supporting.user.command.UserAccountView) {
      tokens.entries.removeIf { it.value == view.email() }
    }
  }
}
