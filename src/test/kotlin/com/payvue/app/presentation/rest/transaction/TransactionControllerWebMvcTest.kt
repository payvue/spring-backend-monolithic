package com.payvue.app.presentation.rest.transaction

import com.payvue.app.application.transaction.TransactionUseCase
import com.payvue.app.application.transaction.result.TransferAssetResult
import com.payvue.app.application.transaction.result.TransferCashResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(TransactionController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class TransactionControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var transactionUseCase: TransactionUseCase

  @MockBean
  private lateinit var tokenProvider: TokenProvider

  @MockBean
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var userDetailsService: UserDetailsService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    beforeTest {
      setupAuthMocks("tester@payvue.com")
    }

    describe("현금 송금 요청") {
      context("인증된 요청이면") {
        it("송금 응답이 반환된다") {
          `when`(transactionUseCase.transferCash(any(UserDetails::class.java), any(), anyString()))
            .thenReturn(TransferCashResult("transfer-1", "COMPLETED", 700L))
          val payload = """
            {"recipientId":"community-1","amount":300,"currency":"KRW","message":"송금","clientRequestId":"client-1"}
          """.trimIndent()

          mockMvc.post("/transaction/transfer/cash") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            header("Idempotency-Key", "idem-1")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.transferId") { value("transfer-1") }
            jsonPath("$.data.status") { value("COMPLETED") }
            jsonPath("$.data.balance") { value(700) }
          }
        }
      }
    }

    describe("사용자 자산 이전 요청") {
      context("인증된 요청이면") {
        it("이전 응답이 반환된다") {
          `when`(transactionUseCase.transferAssetToUser(any(UserDetails::class.java), any(), any(), anyString()))
            .thenReturn(TransferAssetResult("transfer-2", "COMPLETED"))
          val payload = """
            {"assetId":"asset-1","recipientEmail":"to@payvue.com","message":"이체","clientRequestId":"client-2"}
          """.trimIndent()

          mockMvc.post("/transaction/transfer/user/cash") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
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

    describe("커뮤니티 자산 이전 요청") {
      context("인증된 요청이면") {
        it("이전 응답이 반환된다") {
          `when`(transactionUseCase.transferAssetToCommunity(any(UserDetails::class.java), any(), any(), anyString()))
            .thenReturn(TransferAssetResult("transfer-3", "COMPLETED"))
          val payload = """
            {"assetId":"asset-2","groupId":"community-2","message":"이체","clientRequestId":"client-3"}
          """.trimIndent()

          mockMvc.post("/transaction/transfer/community/cash") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            header("Idempotency-Key", "idem-3")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.transferId") { value("transfer-3") }
            jsonPath("$.data.status") { value("COMPLETED") }
          }
        }
      }
    }
  }

  private fun setupAuthMocks(email: String) {
    val userDetails = Mockito.mock(UserDetails::class.java)
    `when`(userDetails.username).thenReturn(email)
    `when`(tokenProvider.validate(anyString())).thenReturn(true)
    `when`(tokenProvider.getSubject(anyString())).thenAnswer { it.arguments[0] as String }
    `when`(sessionTokenRepository.isActive(anyString())).thenReturn(true)
    `when`(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails)
  }
}
