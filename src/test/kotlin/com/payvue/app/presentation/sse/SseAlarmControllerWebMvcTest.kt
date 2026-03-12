package com.payvue.app.presentation.sse

import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.security.SecurityConfiguration
import com.payvue.app.infrastructure.security.SecurityTestConfiguration
import com.payvue.app.infrastructure.sse.SseEmitterRegistry
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import com.payvue.supporting.user.UserContextManager
import com.payvue.supporting.user.command.User
import java.time.LocalDate
import java.util.Optional

@WebMvcTest(SseAlarmController::class)
@Import(SecurityConfiguration::class, SecurityTestConfiguration::class)
class SseAlarmControllerWebMvcTest : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var emitterRegistry: SseEmitterRegistry

  @MockBean
  private lateinit var tokenProvider: TokenProvider

  @MockBean
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var userDetailsService: UserDetailsService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  @MockBean
  private lateinit var userContextManager: UserContextManager

  init {
    beforeTest {
      setupAuthMocks("tester@payvue.com")
      setupUserContext("tester@payvue.com")
    }

    describe("SSE 구독 요청") {
      context("인증된 요청이면") {
        it("SSE 스트림이 반환된다") {
          val emitter = Mockito.mock(SseEmitter::class.java)
          `when`(emitterRegistry.register(anyString())).thenReturn(emitter)

          mockMvc.get("/sse/subscribe") {
            header(HttpHeaders.AUTHORIZATION, "Bearer tester@payvue.com")
          }.andExpect {
            status { isOk() }
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

  private fun setupUserContext(email: String) {
    val user = User.create(
      "login",
      email,
      "010-0000-0000",
      "nick",
      "real",
      LocalDate.of(1990, 1, 1),
      "01",
      "02",
      "road",
      "detail",
      "00000"
    )
    `when`(userContextManager.findByEmail(email, User::class.java))
      .thenReturn(Optional.of(user))
  }
}
