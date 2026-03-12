package com.payvue.app.presentation.sse

import com.payvue.app.application.alarm.AlarmQueryService
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.presentation.rest.alarm.AlarmNotificationList
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
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView
import com.payvue.supporting.user.UserContextManager
import com.payvue.supporting.user.command.User
import java.time.LocalDate
import java.util.Optional
import org.mockito.Mockito.`when`
import java.util.Collections

@SpringBootTest(classes = [com.payvue.PayvueApplication::class, AlarmControllerIT.TestSecurityBeans::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlarmControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  @MockBean
  private lateinit var alarmQueryService: AlarmQueryService

  @MockBean
  private lateinit var userContextManager: UserContextManager

  init {
    describe("알림 컨트롤러 통합 테스트") {
      context("알림 목록 조회 요청에서") {
        it("빈 목록 응답이 반환된다") {
          val token = "alarm-it-token@payvue.com"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val user = User.create(
            "login",
            token,
            "010-0000-0000",
            "nick",
            "real",
            LocalDate.of(1990, 1, 1),
            "01",
            "02",
            "road",
            "detail",
            "00000"
          )
          `when`(userContextManager.findByEmail(token, User::class.java))
            .thenReturn(Optional.of(user))
          `when`(alarmQueryService.fetchReceived(token, 0))
            .thenReturn(AlarmNotificationList(Collections.emptyList()))

          mockMvc.get("/alarm/received/0") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.data.noti.length()") { value(0) }
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
