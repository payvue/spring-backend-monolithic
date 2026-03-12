package com.payvue.app.presentation.rest.hold

import com.payvue.app.application.asset.AssetApplicationService
import com.payvue.app.application.summary.SummaryApplicationService
import com.payvue.app.domain.summary.BalancesByCategory
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
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
import org.springframework.test.web.servlet.get
import java.util.List

@WebMvcTest(AssetController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class AssetControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var assetApplicationService: AssetApplicationService

  @MockBean
  private lateinit var summaryApplicationService: SummaryApplicationService

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

    describe("온라인 자산 요약 조회 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          mockMvc.get("/asset/summary/online")
            .andExpect { status { isUnauthorized() } }
            .andExpect { jsonPath("$.code") { value("UNAUTHORIZED") } }
            .andExpect { jsonPath("$.message") { value("인증이 필요합니다.") } }
        }
      }

      context("인증된 요청이면") {
        it("온라인 자산 목록이 반환된다") {
          `when`(assetApplicationService.online(any(UserDetails::class.java)))
            .thenReturn(
              listOf(
                AssetOnlineItemResponse(
                  "asset-1",
                  "payvue 캐시",
                  "CASH",
                  1200L,
                  1200L,
                  null,
                  null,
                  "ACTIVE",
                  null
                )
              )
            )

          mockMvc.get("/asset/summary/online") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data[0].id") { value("asset-1") }
            jsonPath("$.data[0].name") { value("payvue 캐시") }
            jsonPath("$.data[0].category") { value("CASH") }
            jsonPath("$.data[0].amount") { value(1200) }
            jsonPath("$.data[0].balance") { value(1200) }
            jsonPath("$.data[0].remainingAmount") { value(nullValue()) }
            jsonPath("$.data[0].purchaseDate") { value(nullValue()) }
            jsonPath("$.data[0].status") { value("ACTIVE") }
            jsonPath("$.data[0].description") { value(nullValue()) }
          }
        }
      }
    }

    describe("자산 요약 조회 요청") {
      context("인증된 요청이면") {
        it("자산 요약 정보가 반환된다") {
          val summary = BalancesByCategory.ofAggregate(
            1000L,
            1000L,
            1500L,
            700L,
            700L,
            900L,
            300L,
            300L,
            600L,
            mapOf("GIFT" to 500L, "MUSIC" to 1000L),
            "KRW"
          )
          `when`(summaryApplicationService.accumulate(any(UserDetails::class.java), any()))
            .thenReturn(summary)

          mockMvc.get("/asset/summary") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.total") { value(1000) }
            jsonPath("$.data.totalValue") { value(1000) }
            jsonPath("$.data.totalAssetValue") { value(1500) }
            jsonPath("$.data.personal") { value(700) }
            jsonPath("$.data.personalValue") { value(700) }
            jsonPath("$.data.personalAssetValue") { value(900) }
            jsonPath("$.data.shared") { value(300) }
            jsonPath("$.data.sharedValue") { value(300) }
            jsonPath("$.data.sharedAssetValue") { value(600) }
            jsonPath("$.data.monthlyChangeRate") { value(0.0) }
            jsonPath("$.data.changeRate") { value(0.0) }
            jsonPath("$.data.monthlyChange") { value(0) }
            jsonPath("$.data.currency") { value("KRW") }
            jsonPath("$.data.categories[0].category") { value("GIFT") }
            jsonPath("$.data.categories[0].amount") { value(500) }
            jsonPath("$.data.categories[0].ratio") { value(33) }
            jsonPath("$.data.categories[1].category") { value("MUSIC") }
            jsonPath("$.data.categories[1].amount") { value(1000) }
            jsonPath("$.data.categories[1].ratio") { value(66) }
          }
        }
      }
    }

    describe("결제 수단 조회 요청") {
      context("인증된 요청이면") {
        it("결제 수단 목록이 반환된다") {
          `when`(assetApplicationService.paymentMethods(any(UserDetails::class.java)))
            .thenReturn(
              listOf(
                PaymentMethodResponse(
                  "pm-001",
                  "card",
                  "결제 카드",
                  "신한카드",
                  true,
                  "잔액 35,000원"
                )
              )
            )

          mockMvc.get("/asset/payment/methods") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("pm-001") }
            jsonPath("$.data[0].type") { value("card") }
            jsonPath("$.data[0].name") { value("결제 카드") }
            jsonPath("$.data[0].provider") { value("신한카드") }
            jsonPath("$.data[0].isDefault") { value(true) }
            jsonPath("$.data[0].details") { value("잔액 35,000원") }
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
