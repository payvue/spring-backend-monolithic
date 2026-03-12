package com.payvue.app.presentation.rest.transaction

import com.payvue.app.application.transaction.TransactionHistoryResult
import com.payvue.app.application.transaction.TransactionQueryUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.core.community.CommunityId
import com.payvue.shared.Period
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TransactionQueryControllerIT.TestSecurityBeans::class)
class TransactionQueryControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var transactionQueryUseCase: TransactionQueryUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("거래 조회 컨트롤러 통합 테스트") {
      context("이력 조회 요청에서") {
        it("거래 내역이 반환된다") {
          val token = "tx-query-it-token"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          `when`(transactionQueryUseCase.queryPersonal(any(), any(), any(), any(), any()))
            .thenReturn(sampleResult())

          mockMvc.get("/transaction/query/history?count=1&limit=5") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.items[0].id") { value("tx-1") }
            jsonPath("$.data.pagination.total") { value(1) }
          }
        }
      }

      context("커뮤니티 거래 조회 요청에서") {
        it("거래 내역이 반환된다") {
          val token = "tx-query-it-token-2"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          `when`(transactionQueryUseCase.queryCommunity(any(CommunityId::class.java), any(), any(), any(), any(Period::class.java)))
            .thenReturn(sampleResult())
          val id = UUID.randomUUID()

          mockMvc.get("/transaction/query/community/$id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.items[0].id") { value("tx-1") }
          }
        }
      }
    }
  }

  private fun sampleResult(): TransactionHistoryResult {
    return TransactionHistoryResult(
      listOf(
        TransactionHistoryResult.Item(
          "tx-1",
          "transfer",
          1000L,
          "desc",
          "message",
          "payer",
          "2024-01-01T00:00:00Z",
          "completed",
          "cash",
          "app",
          "seoul",
          "10:00",
          "merchant-1",
          0L,
          "card"
        )
      ),
      TransactionHistoryResult.Pagination(1, 20, 1)
    )
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
