package com.payvue.app.presentation.rest.community

import com.payvue.app.application.community.PaymentRequestUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PaymentRequestControllerIT.TestSecurityBeans::class)
class PaymentRequestControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var paymentRequestUseCase: PaymentRequestUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("결제 요청 컨트롤러 통합 테스트") {
      context("결제 요청 생성 요청에서") {
        it("요청 정보가 반환된다") {
          val token = "payment-it-token"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val id = UUID.randomUUID().toString()
          val response = PaymentRequestResponse(
            id,
            "community-1",
            "requester-1",
            "requester-name",
            1000L,
            "회비 요청",
            "PAYMENT",
            "HIGH",
            "PENDING",
            Instant.parse("2024-01-01T00:00:00Z"),
            null,
            null,
            null
          )
          `when`(paymentRequestUseCase.create(any(), any(), any(), any()))
            .thenReturn(response)

          val payload = """
            {"amount":1000,"description":"회비 요청","category":"PAYMENT","urgency":"HIGH"}
          """.trimIndent()

          mockMvc.post("/community/$id/payment-request?category=COOPERATIVE") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.id") { value(id) }
            jsonPath("$.data.amount") { value(1000) }
            jsonPath("$.data.status") { value("PENDING") }
          }
        }
      }

      context("결제 요청 목록 조회 요청에서") {
        it("목록 응답이 반환된다") {
          val token = "payment-it-token-2"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val id = UUID.randomUUID().toString()
          val response = PaymentRequestResponse(
            "req-1",
            "community-1",
            "requester-1",
            "requester-name",
            500L,
            "설명",
            "PAYMENT",
            "LOW",
            "PENDING",
            Instant.parse("2024-01-01T00:00:00Z"),
            null,
            null,
            null
          )
          `when`(paymentRequestUseCase.list(any(), any(), any()))
            .thenReturn(listOf(response))

          mockMvc.get("/community/$id/payment-requests") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("category", CommunityCategory.COOPERATIVE.name)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("req-1") }
            jsonPath("$.data[0].amount") { value(500) }
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
