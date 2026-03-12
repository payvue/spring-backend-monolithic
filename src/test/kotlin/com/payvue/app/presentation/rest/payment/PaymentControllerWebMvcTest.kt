package com.payvue.app.presentation.rest.payment

import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.payment.PaymentProvider
import com.payvue.app.application.pay.PayApplicationService
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import com.payvue.generic.payment.Payment
import com.payvue.generic.payment.PaymentModeType
import com.payvue.generic.payment.PaymentStatus
import com.payvue.generic.payment.ProviderId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.containsString
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
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
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(PaymentController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class PaymentControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var payApplicationService: PayApplicationService

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

    describe("결제 예약 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          val payload = """{"name":"order","type":"PAYMENT","price":10000,"amount":1}"""

          mockMvc.post("/payment/KAKAO/SINGLE/reserve") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("인증이 필요합니다.") }
          }
        }
      }

      context("인증된 요청이면") {
        it("예약 응답이 반환된다") {
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
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.reserve") { value("https://redirect.example/pay") }
          }
        }
      }
    }

    describe("결제 승인 확인 요청") {
      context("승인이 완료된 결제라면") {
        it("완료 메시지가 반환된다") {
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
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            param("resultCode", "Success")
          }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
            content { string(org.hamcrest.Matchers.containsString("payment:success")) }
          }
        }
      }

      context("승인 실패라면") {
        it("실패 메시지가 반환된다") {
          val paymentId = UUID.randomUUID().toString()
          mockMvc.get("/payment/redirect/confirm/$paymentId") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            param("resultCode", "Fail")
          }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
            content { string(containsString("window.close()")) }
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
