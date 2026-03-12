package com.payvue.app.presentation.rest.transaction

import com.payvue.app.application.transaction.TransactionHistoryResult
import com.payvue.app.application.transaction.TransactionQueryUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import com.payvue.core.community.CommunityId
import com.payvue.core.transaction.command.TransactionType
import com.payvue.shared.Period
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(TransactionQueryController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class TransactionQueryControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var transactionQueryUseCase: TransactionQueryUseCase

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

    describe("개인 자산 거래 조회 요청") {
      context("인증된 요청이면") {
        it("거래 내역이 반환된다") {
          `when`(
            transactionQueryUseCase.queryPersonal(
              any(UserDetails::class.java),
              anyString(),
              any(TransactionType::class.java),
              any(),
              any(Period::class.java)
            )
          ).thenReturn(sampleResult())

          mockMvc.get("/transaction/query/personal/asset") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.items[0].id") { value("tx-1") }
            jsonPath("$.data.items[0].kind") { value("transfer") }
            jsonPath("$.data.items[0].amount") { value(1000) }
            jsonPath("$.data.items[0].status") { value("completed") }
            jsonPath("$.data.pagination.total") { value(1) }
          }
        }
      }
    }

    describe("최근 거래 조회 요청") {
      context("인증된 요청이면") {
        it("거래 내역이 반환된다") {
          `when`(transactionQueryUseCase.queryPersonal(any(), anyString(), any(), any(), any()))
            .thenReturn(sampleResult())

          mockMvc.get("/transaction/query/personal/recent") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.items[0].id") { value("tx-1") }
          }
        }
      }
    }

    describe("커뮤니티 거래 조회 요청") {
      context("인증된 요청이면") {
        it("거래 내역이 반환된다") {
          `when`(transactionQueryUseCase.queryCommunity(any(CommunityId::class.java), anyString(), any(), any(), any()))
            .thenReturn(sampleResult())
          val id = UUID.randomUUID()

          mockMvc.get("/transaction/query/community/$id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.items[0].id") { value("tx-1") }
          }
        }
      }
    }

    describe("이력 조회 요청") {
      context("인증된 요청이면") {
        it("거래 내역이 반환된다") {
          `when`(transactionQueryUseCase.queryPersonal(any(), anyString(), any(), any(), any()))
            .thenReturn(sampleResult())

          mockMvc.get("/transaction/query/history?count=1&limit=5") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.pagination.total") { value(1) }
          }
        }
      }
    }

    describe("자산별 거래 조회 요청") {
      context("인증된 요청이면") {
        it("거래 내역이 반환된다") {
          `when`(transactionQueryUseCase.queryPersonal(any(), anyString(), any(), any(), any()))
            .thenReturn(sampleResult())

          mockMvc.get("/transaction/query/cash/transfer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
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

  private fun setupAuthMocks(email: String) {
    val userDetails = Mockito.mock(UserDetails::class.java)
    `when`(userDetails.username).thenReturn(email)
    `when`(tokenProvider.validate(anyString())).thenReturn(true)
    `when`(tokenProvider.getSubject(anyString())).thenAnswer { it.arguments[0] as String }
    `when`(sessionTokenRepository.isActive(anyString())).thenReturn(true)
    `when`(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails)
  }
}
