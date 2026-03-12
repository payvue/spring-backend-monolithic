package com.payvue.app.presentation.rest.payment

import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RecurrentControllerIT.TestSecurityBeans::class)
class RecurrentControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("정기 결제 컨트롤러 통합 테스트") {
      context("자동 충전 조건 조회 요청에서") {
        it("조건 목록이 반환된다") {
          val token = "recurrent-it-token"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))

          mockMvc.get("/recurrent/condition/charge") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("auto-001") }
            jsonPath("$.data[0].name") { value("잔액 1만원 이하 자동 충전") }
            jsonPath("$.data[0].paymentMethod") { value("신한카드") }
            jsonPath("$.data[0].isActive") { value(true) }
            jsonPath("$.data[0].chargeAmount") { value(30000) }
            jsonPath("$.data[0].strategy.triggerType") { value("balance_below") }
          }
        }
      }

      context("정기 결제 조건 조회 요청에서") {
        it("조건 목록이 반환된다") {
          val token = "recurrent-it-token-2"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))

          mockMvc.get("/recurrent/condition/scheduled") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("1") }
            jsonPath("$.data[0].name") { value("넷플릭스 프리미엄") }
            jsonPath("$.data[0].merchantName") { value("넷플릭스 프리미엄") }
            jsonPath("$.data[0].paymentMethod") { value("PayEasy 잔액") }
            jsonPath("$.data[0].lastPaymentDate") { value("2024-01-15") }
            jsonPath("$.data[0].nextPaymentDate") { value("2024-02-15") }
            jsonPath("$.data[0].failureReason") { value(nullValue()) }
            jsonPath("$.data[0].strategy.scheduleType") { value("monthly") }
            jsonPath("$.data[0].isActive") { value(true) }
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
