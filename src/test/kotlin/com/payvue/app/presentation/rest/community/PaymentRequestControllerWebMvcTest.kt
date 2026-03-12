package com.payvue.app.presentation.rest.community

import com.payvue.app.application.community.PaymentRequestUseCase
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.community.CommunityCategory
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.util.UUID

@WebMvcTest(PaymentRequestController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class PaymentRequestControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var paymentRequestUseCase: PaymentRequestUseCase

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

    describe("결제 요청 생성 요청") {
      context("인증된 요청이면") {
        it("요청 정보가 반환된다") {
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
          `when`(paymentRequestUseCase.create(any(UserDetails::class.java), any(), any(), any()))
            .thenReturn(response)

          val payload = """
            {"amount":1000,"description":"회비 요청","category":"PAYMENT","urgency":"HIGH"}
          """.trimIndent()

          mockMvc.post("/community/$id/payment-request?category=COOPERATIVE") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.id") { value(id) }
            jsonPath("$.data.groupId") { value("community-1") }
            jsonPath("$.data.requesterId") { value("requester-1") }
            jsonPath("$.data.amount") { value(1000) }
            jsonPath("$.data.description") { value("회비 요청") }
            jsonPath("$.data.category") { value("PAYMENT") }
            jsonPath("$.data.urgency") { value("HIGH") }
            jsonPath("$.data.status") { value("PENDING") }
            jsonPath("$.data.requestDate") { value("2024-01-01T00:00:00Z") }
            jsonPath("$.data.approvedDate") { value(nullValue()) }
            jsonPath("$.data.rejectedDate") { value(nullValue()) }
            jsonPath("$.data.rejectionReason") { value(nullValue()) }
          }
        }
      }
    }

    describe("결제 요청 목록 조회 요청") {
      context("인증된 요청이면") {
        it("목록 응답이 반환된다") {
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
          `when`(paymentRequestUseCase.list(any(UserDetails::class.java), any(), any()))
            .thenReturn(listOf(response))

          mockMvc.get("/community/$id/payment-requests") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
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

    describe("내 결제 요청 조회 요청") {
      context("인증된 요청이면") {
        it("내 요청 목록이 반환된다") {
          val response = PaymentRequestResponse(
            "req-2",
            "community-2",
            "requester-2",
            "requester-name",
            700L,
            "설명",
            "PAYMENT",
            "HIGH",
            "PENDING",
            Instant.parse("2024-01-01T00:00:00Z"),
            null,
            null,
            null
          )
          `when`(paymentRequestUseCase.listMy(any(UserDetails::class.java)))
            .thenReturn(listOf(response))

          mockMvc.get("/community/payment-requests/my") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            param("communityId", UUID.randomUUID().toString())
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("req-2") }
            jsonPath("$.data[0].requesterId") { value("requester-2") }
          }
        }
      }
    }

    describe("결제 요청 승인 요청") {
      context("인증된 요청이면") {
        it("성공 응답이 반환된다") {
          mockMvc.put("/community/payment-request/req-3/approve?communityCategory=COOPERATIVE") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }

          verify(paymentRequestUseCase).approve(any(UserDetails::class.java), anyString(), any(), any())
        }
      }
    }

    describe("결제 요청 거절 요청") {
      context("인증된 요청이면") {
        it("성공 응답이 반환된다") {
          val payload = """{"reason":"사유"}"""

          mockMvc.put("/community/payment-request/req-4/reject?category=COOPERATIVE") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }

          verify(paymentRequestUseCase).reject(any(UserDetails::class.java), anyString(), any(), any())
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
