package com.payvue.app.presentation.rest.account

import com.fasterxml.jackson.databind.ObjectMapper
import com.payvue.app.application.account.AccountUseCase
import com.payvue.app.application.account.query.AccountExistResult
import com.payvue.app.application.account.query.AccountInfoResult
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.verify
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
import org.springframework.test.web.servlet.put
import java.time.LocalDate
import java.util.Optional

@WebMvcTest(AccountController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class AccountControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @MockBean
  private lateinit var accountUseCase: AccountUseCase

  @MockBean
  private lateinit var userRepository: UserRepository

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

    describe("계정 생성 요청") {
      context("유효한 요청이 들어오면") {
        it("성공 응답을 반환한다") {
          val request = AccountCreateRequest(
            "alias",
            "login",
            "real-name",
            "881010-1234567",
            1990,
            1,
            2,
            "user@payvue.com",
            "010-1234-5678",
            "Passw0rd!",
            "Passw0rd!",
            "11",
            "110",
            "road",
            "12345",
            "detail"
          )

          mockMvc.post("/user/account/create") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value("인증 메일이 발송되었습니다.") }
          }

          verify(accountUseCase).create(any())
        }
      }
    }

    describe("계정 존재 조회 요청") {
      context("사용자가 존재하면") {
        it("이름과 연락처가 반환된다") {
          `when`(accountUseCase.exist(any()))
            .thenReturn(AccountExistResult("name", AccountExistResult.Contact("exist@payvue.com", "010-1111-2222")))

          val request = AccountExistRequest("exist@payvue.com")

          mockMvc.post("/user/account/exist") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.name") { value("name") }
            jsonPath("$.data.contact.email") { value("exist@payvue.com") }
            jsonPath("$.data.contact.phone") { value("010-1111-2222") }
          }
        }
      }
    }

    describe("계정 수정 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          val request = AccountEditRequest("new-name", "010-9999-0000")

          mockMvc.put("/user/account/edit") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("인증이 필요합니다.") }
          }
        }
      }

      context("인증된 요청이면") {
        it("성공 응답을 반환한다") {
          val request = AccountEditRequest("new-name", "010-9999-0000")

          mockMvc.put("/user/account/edit") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }

          verify(accountUseCase).edit(any(UserDetails::class.java), any())
        }
      }
    }

    describe("비밀번호 변경 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          val request = ChangePasswordRequest("old", "new")

          mockMvc.put("/user/account/change-pw") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.message") { value("인증이 필요합니다.") }
          }
        }
      }

      context("인증된 요청이면") {
        it("성공 응답을 반환한다") {
          val request = ChangePasswordRequest("old", "new")

          mockMvc.put("/user/account/change-pw") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.message") { value("요청하신 응답이 완료되었습니다.") }
            jsonPath("$.data") { value(nullValue()) }
          }

          verify(accountUseCase).changePassword(any(UserDetails::class.java), any())
        }
      }
    }

    describe("계정 정보 조회 요청") {
      context("인증 없이 접근하면") {
        it("인증 실패 응답이 반환된다") {
          mockMvc.get("/user/account/info")
            .andExpect { status { isUnauthorized() } }
            .andExpect { jsonPath("$.code") { value("UNAUTHORIZED") } }
            .andExpect { jsonPath("$.message") { value("인증이 필요합니다.") } }
        }
      }

      context("인증된 요청이면") {
        it("계정 정보가 반환된다") {
          `when`(accountUseCase.info(any(UserDetails::class.java)))
            .thenReturn(
              AccountInfoResult(
                AccountInfoResult.Account("alias", "id", AccountInfoResult.Status("now", "join")),
                AccountInfoResult.Info(
                  "name",
                  AccountInfoResult.Contact("tester@payvue.com", "010-0000-0000"),
                  "avatar"
                ),
                AccountInfoResult.Asset(100L, 20L)
              )
            )

          mockMvc.get("/user/account/info") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.account.alias") { value("alias") }
            jsonPath("$.data.account.id") { value("id") }
            jsonPath("$.data.account.status.lastLoggedIn") { value("now") }
            jsonPath("$.data.account.status.joinAt") { value("join") }
            jsonPath("$.data.info.name") { value("name") }
            jsonPath("$.data.info.contact.email") { value("tester@payvue.com") }
            jsonPath("$.data.info.contact.phone") { value("010-0000-0000") }
            jsonPath("$.data.info.avatar") { value("avatar") }
            jsonPath("$.data.asset.balance") { value(100) }
            jsonPath("$.data.asset.point") { value(20) }
          }
        }
      }
    }

    describe("프로필 조회 요청") {
      context("인증된 요청이고 사용자가 존재하면") {
        it("프로필이 반환된다") {
          val user = createUser("profile@payvue.com")
          `when`(userRepository.findByEmail("profile@payvue.com")).thenReturn(Optional.of(user))

          mockMvc.get("/user/account/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            param("email", "profile@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.id") { value(user.getId().toString()) }
            jsonPath("$.data.name") { value("real-name") }
            jsonPath("$.data.email") { value("profile@payvue.com") }
            jsonPath("$.data.avatar") { value(nullValue()) }
            jsonPath("$.data.status") { value("PENDING") }
          }
        }
      }

      context("사용자가 존재하지 않으면") {
        it("NOT_FOUND 응답을 반환한다") {
          `when`(userRepository.findByEmail("missing@payvue.com")).thenReturn(Optional.empty())

          mockMvc.get("/user/account/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
            param("email", "missing@payvue.com")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("NOT_FOUND") }
            jsonPath("$.message") { value("유저를 찾을 수 없습니다.") }
            jsonPath("$.data") { value(nullValue()) }
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

  private fun createUser(email: String): User {
    return User.create(
      "login",
      email,
      "010-9999-8888",
      "nickname",
      "real-name",
      LocalDate.of(1990, 1, 1),
      "11",
      "110",
      "road",
      "detail",
      "12345",
      "encoded",
      UserPasswordGuard.defaultPolicy("Passw0rd!")
    )
  }
}
