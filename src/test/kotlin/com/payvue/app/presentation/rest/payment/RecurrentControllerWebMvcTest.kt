package com.payvue.app.presentation.rest.payment

import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
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

@WebMvcTest(RecurrentController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class RecurrentControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

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

    describe("자동 충전 조건 조회 요청") {
      context("인증된 요청이면") {
        it("자동 충전 조건 목록이 반환된다") {
          mockMvc.get("/recurrent/condition/charge") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data[0].id") { value("auto-001") }
            jsonPath("$.data[0].name") { value("잔액 1만원 이하 자동 충전") }
            jsonPath("$.data[0].paymentMethod") { value("신한카드") }
            jsonPath("$.data[0].isActive") { value(true) }
            jsonPath("$.data[0].chargeAmount") { value(30000) }
            jsonPath("$.data[0].strategy.type") { value("condition") }
            jsonPath("$.data[0].strategy.triggerType") { value("balance_below") }
            jsonPath("$.data[0].strategy.triggerValue") { value(10000) }
            jsonPath("$.data[0].strategy.description") { value("잔액 부족 시 자동 충전") }
          }
        }
      }
    }

    describe("정기 결제 조건 조회 요청") {
      context("인증된 요청이면") {
        it("정기 결제 조건 목록이 반환된다") {
          mockMvc.get("/recurrent/condition/scheduled") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data[0].id") { value("1") }
            jsonPath("$.data[0].name") { value("넷플릭스 프리미엄") }
            jsonPath("$.data[0].merchantName") { value("넷플릭스 프리미엄") }
            jsonPath("$.data[0].amount") { value(17000) }
            jsonPath("$.data[0].paymentMethod") { value("PayEasy 잔액") }
            jsonPath("$.data[0].status") { value("active") }
            jsonPath("$.data[0].lastPaymentDate") { value("2024-01-15") }
            jsonPath("$.data[0].nextPaymentDate") { value("2024-02-15") }
            jsonPath("$.data[0].failureReason") { value(nullValue()) }
            jsonPath("$.data[0].strategy.type") { value("schedule") }
            jsonPath("$.data[0].strategy.scheduleType") { value("monthly") }
            jsonPath("$.data[0].isActive") { value(true) }
            jsonPath("$.data[0].nextTrigger") { value("다음 결제: 2024-02-15") }
            jsonPath("$.data[0].lastTriggered") { value("2024-01-15") }
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
