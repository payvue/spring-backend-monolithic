package com.payvue.app.presentation.rest.transaction

import com.payvue.app.application.transaction.TransactionUseCase
import com.payvue.app.application.transaction.result.TransferAssetResult
import com.payvue.app.application.transaction.result.TransferCashResult
import com.payvue.app.domain.auth.AuthContextManager
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
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TransactionControllerIT.TestSecurityBeans::class)
class TransactionControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var transactionUseCase: TransactionUseCase

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("거래 컨트롤러 통합 테스트") {
      context("현금 송금 요청에서") {
        it("송금 응답이 반환된다") {
          val token = "tx-it-token"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          `when`(transactionUseCase.transferCash(any(), any(), any()))
            .thenReturn(TransferCashResult("transfer-1", "COMPLETED", 700L))
          val payload = """
            {"recipientId":"community-1","amount":300,"currency":"KRW","message":"송금","clientRequestId":"client-1"}
          """.trimIndent()

          mockMvc.post("/transaction/transfer/cash") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            header("Idempotency-Key", "idem-1")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.transferId") { value("transfer-1") }
            jsonPath("$.data.balance") { value(700) }
          }
        }
      }

      context("사용자 자산 이전 요청에서") {
        it("이전 응답이 반환된다") {
          val token = "tx-it-token-2"
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          `when`(transactionUseCase.transferAssetToUser(any(), any(), any(), any()))
            .thenReturn(TransferAssetResult("transfer-2", "COMPLETED"))
          val payload = """
            {"assetId":"asset-1","recipientEmail":"to@payvue.com","message":"이체","clientRequestId":"client-2"}
          """.trimIndent()

          mockMvc.post("/transaction/transfer/user/cash") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            header("Idempotency-Key", "idem-2")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.transferId") { value("transfer-2") }
            jsonPath("$.data.status") { value("COMPLETED") }
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
