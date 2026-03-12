package com.payvue.app.presentation.rest.account

import com.fasterxml.jackson.databind.ObjectMapper
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AccountControllerIT.TestSecurityBeans::class)
class AccountControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("계정 컨트롤러 통합 테스트") {
      context("프로필 조회 요청에서") {
        it("인증된 요청이면 프로필이 반환된다") {
          val user = createAndPersistUser("profile-it@payvue.com")
          val token = UserAccountView.from(user).email()
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))

          mockMvc.get("/user/account/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("email", "profile-it@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.id") { value(user.getId().toString()) }
            jsonPath("$.data.name") { value("real-name") }
            jsonPath("$.data.email") { value("profile-it@payvue.com") }
            jsonPath("$.data.avatar") { value(nullValue()) }
            jsonPath("$.data.status") { value("PENDING") }
          }
        }
      }

      context("계정 존재 조회 요청에서") {
        it("사용자가 없으면 null 결과가 반환된다") {
          val request = AccountExistRequest("none-it@payvue.com")

          mockMvc.post("/user/account/exist") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.name") { value(nullValue()) }
            jsonPath("$.data.contact") { value(nullValue()) }
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
