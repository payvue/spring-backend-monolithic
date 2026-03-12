package com.payvue.app.presentation.rest.community

import com.payvue.app.application.community.CommunityUseCase
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.hamcrest.Matchers.nullValue

@WebMvcTest(CommunityController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class CommunityControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var communityUseCase: CommunityUseCase

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

    describe("커뮤니티 참여 조회 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          mockMvc.get("/community/participating")
            .andExpect { status { isUnauthorized() } }
            .andExpect { jsonPath("$.code") { value("UNAUTHORIZED") } }
            .andExpect { jsonPath("$.message") { value("인증이 필요합니다.") } }
        }
      }

      context("인증된 요청이면") {
        it("참여 목록 응답이 반환된다") {
          val response = CommunityParticipatingResponse(
            false,
            listOf(
              CommunityParticipatingResponse.Group(
                "community-1",
                "가족 계정",
                listOf(
                  CommunityParticipatingResponse.Member(
                    "member-1",
                    "ADMIN",
                    CommunityParticipatingResponse.Status(true, "2024-01-01T00:00:00Z"),
                    CommunityParticipatingResponse.Info(
                      "홍길동",
                      CommunityParticipatingResponse.Contact("tester@payvue.com", "01012341234")
                    ),
                    CommunityParticipatingResponse.Contribution(0L, "2024-01-01T00:00:00Z"),
                    CommunityParticipatingResponse.Policy(true)
                  )
                ),
                CommunityParticipatingResponse.Shared(150000L)
              )
            )
          )
          `when`(communityUseCase.participating(any(UserDetails::class.java), any()))
            .thenReturn(response)

          mockMvc.get("/community/participating") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.hasIssue") { value(false) }
            jsonPath("$.data.groups[0].id") { value("community-1") }
            jsonPath("$.data.groups[0].name") { value("가족 계정") }
            jsonPath("$.data.groups[0].members[0].id") { value("member-1") }
            jsonPath("$.data.groups[0].members[0].role") { value("ADMIN") }
            jsonPath("$.data.groups[0].members[0].status.isLoggedIn") { value(true) }
            jsonPath("$.data.groups[0].members[0].status.lastLoggedIn") { value("2024-01-01T00:00:00Z") }
            jsonPath("$.data.groups[0].members[0].info.name") { value("홍길동") }
            jsonPath("$.data.groups[0].members[0].info.contact.email") { value("tester@payvue.com") }
            jsonPath("$.data.groups[0].members[0].info.contact.phone") { value("01012341234") }
            jsonPath("$.data.groups[0].members[0].contribution.amount") { value(0) }
            jsonPath("$.data.groups[0].members[0].contribution.joinDate") { value("2024-01-01T00:00:00Z") }
            jsonPath("$.data.groups[0].members[0].policy.active") { value(true) }
            jsonPath("$.data.groups[0].shared.balance") { value(150000) }
          }
        }
      }
    }

    describe("커뮤니티 생성 요청") {
      context("인증된 요청이면") {
        it("성공 응답이 반환된다") {
          val payload = """
            {
              "name":"가족 계정",
              "description":"가족 커뮤니티",
              "initialAmount":10000,
              "invitedMembers":["member@payvue.com"]
            }
          """.trimIndent()

          mockMvc.post("/community/create?standAlone=false") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data.communityId") { value(nullValue()) }
          }

          verify(communityUseCase).create(any(UserDetails::class.java), any(), any())
        }
      }
    }

    describe("커뮤니티 초대 요청") {
      context("인증된 요청이면") {
        it("성공 응답이 반환된다") {
          val payload = """
            ["member@payvue.com","member2@payvue.com"]
          """.trimIndent()

          mockMvc.post("/community/invite/00000000-0000-0000-0000-000000000000") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = payload
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }

          verify(communityUseCase).invite(any(UserDetails::class.java), any(), any())
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
