package com.payvue.app.presentation.rest.payment

import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.payment.PaymentProvider
import com.payvue.app.application.pay.PayApplicationService
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.generic.payment.Payment
import com.payvue.generic.payment.PaymentModeType
import com.payvue.generic.payment.PaymentStatus
import com.payvue.generic.payment.ProviderId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.containsString
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PaymentControllerIT.TestSecurityBeans::class)
class PaymentControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var payApplicationService: PayApplicationService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("결제 컨트롤러 통합 테스트") {
      context("결제 예약 요청에서") {
        it("예약 응답이 반환된다") {
          val token = "payment-it-token"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val payment = Payment.reconstitute(
            com.payvue.generic.payment.PaymentId.generate(),
            PaymentStatus.PENDING,
            PaymentModeType.SINGLE,
            ProviderId(PaymentProvider.KAKAO.name),
            null,
            mapOf("redirectUrl" to "https://redirect.example/pay")
          )
          `when`(payApplicationService.reserve(any(), any(), any(), any(), any())).thenReturn(payment)
          val payload = """{"name":"order","type":"PAYMENT","price":10000,"amount":1}"""

          mockMvc.post("/payment/KAKAO/SINGLE/reserve") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.reserve") { value("https://redirect.example/pay") }
          }
        }
      }

      context("결제 승인 확인 요청에서") {
        it("승인 완료 메시지가 반환된다") {
          val token = "payment-it-token-2"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val paymentId = UUID.randomUUID().toString()
          val payment = Payment.reconstitute(
            com.payvue.generic.payment.PaymentId.of(paymentId),
            PaymentStatus.APPROVED,
            PaymentModeType.SINGLE,
            ProviderId(PaymentProvider.KAKAO.name),
            null,
            mapOf()
          )
          `when`(payApplicationService.complete(any(), any(), any(), any())).thenReturn(payment)

          mockMvc.get("/payment/redirect/confirm/$paymentId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("resultCode", "Success")
          }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
            content { string(containsString("payment:success")) }
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
