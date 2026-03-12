package com.payvue.app.application.auth

import com.payvue.app.application.auth.login.LoginCommand
import com.payvue.app.domain.auth.AuthContextConfiguration
import com.payvue.app.domain.auth.VerificationPurpose
import com.payvue.app.domain.auth.VerificationTokenRepository
import com.payvue.app.domain.auth.VerificationTokenService
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.mail.EmailSender
import com.payvue.app.infrastructure.mail.EmailTemplate
import com.payvue.app.infrastructure.mail.EmailTemplateId
import com.payvue.app.infrastructure.mail.EmailTemplateResolver
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.generic.alarm.AlarmContextConfiguration
import com.payvue.generic.alarm.AlarmContextManager
import com.payvue.supporting.user.UserContextConfiguration
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserAccountView
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Instant
import java.util.Date
import org.springframework.context.annotation.FilterType
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

@Testcontainers
@SpringBootTest(classes = [VerificationApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class VerificationApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  companion object {
    @Container
    @JvmStatic
    val redis: GenericContainer<*> = GenericContainer(
      DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379)

    init {
      if (!redis.isRunning) {
        redis.start()
      }
    }

    @JvmStatic
    @DynamicPropertySource
    fun redisProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.data.redis.host") { redis.host }
      registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
    }
  }

  @Autowired
  private lateinit var service: VerificationApplicationService

  @Autowired
  private lateinit var authService: AuthApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var verificationTokenService: VerificationTokenService

  @Autowired
  private lateinit var verificationTokenRepository: VerificationTokenRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @Autowired
  private lateinit var passwordEncoder: PasswordEncoder

  @MockBean
  private lateinit var emailSender: EmailSender

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  init {
    beforeTest {
      Mockito.reset(emailSender)
      redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    given("회원가입 인증 요청에서") {
      `when`("인증 토큰이 유효하고 미인증 사용자일 때") {
        then("사용자가 활성화되고 개인 커뮤니티가 생성된다") {
          val user = createAndPersistUser("signup@payvue.com", "Passw0rd!")
          val token = verificationTokenService.generateToken(user, VerificationPurpose.SIGNUP)

          service.verifySignup(token.getToken())

          val stored = userRepository.findByEmail("signup@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)
          view.emailVerified() shouldBe true
          view.status().name shouldBe "ACTIVE"
          view.membershipsByCommunity().shouldNotBeEmpty()
        }
      }

      `when`("이미 인증된 사용자일 때") {
        then("예외 없이 종료되고 커뮤니티가 추가되지 않는다") {
          val user = createAndPersistUser("verified@payvue.com", "Passw0rd!")
          user.verifyEmail()
          user.activate()
          userRepository.save(user)
          val token = verificationTokenService.generateToken(user, VerificationPurpose.SIGNUP)

          service.verifySignup(token.getToken())

          val stored = userRepository.findByEmail("verified@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)
          view.emailVerified() shouldBe true
          view.membershipsByCommunity().shouldBeEmpty()
        }
      }

      `when`("토큰이 유효하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.verifySignup("invalid-token") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("아이디 찾기 인증 요청에서") {
      `when`("토큰이 유효할 때") {
        then("마스킹된 아이디 결과가 반환된다") {
          val user = createAndPersistUser("ab@payvue.com", "Passw0rd!", "login-id")
          val token = verificationTokenService.generateToken(user, VerificationPurpose.FIND_ID)

          val result = service.verifyFindId(token.getToken())

          result.loginId() shouldBe "login-id"
          result.maskedEmail() shouldBe "a***@payvue.com"
        }
      }

      `when`("토큰이 유효하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.verifyFindId("invalid-token") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("비밀번호 재설정 인증 요청에서") {
      `when`("토큰이 유효할 때") {
        then("임시 비밀번호가 발급되고 로그인에 사용할 수 있다") {
          val user = createAndPersistUser("reset@payvue.com", "Passw0rd!")
          val token = verificationTokenService.generateToken(user, VerificationPurpose.RESET_PASSWORD)

          val result = service.verifyResetPassword(token.getToken())

          val loginResult = authService.login(LoginCommand("reset@payvue.com", result.temporaryPassword()))
          loginResult.accessToken().shouldNotBeBlank()
        }
      }

      `when`("토큰이 유효하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.verifyResetPassword("invalid-token") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("비밀번호 재설정 확정 요청에서") {
      `when`("토큰이 유효하고 새 비밀번호가 제공될 때") {
        then("새 비밀번호로 로그인할 수 있다") {
          val user = createAndPersistUser("confirm@payvue.com", "Passw0rd!")
          val token = verificationTokenService.generateToken(user, VerificationPurpose.RESET_PASSWORD)

          service.resetPasswordConfirm(token.getToken(), "new-password")

          val loginResult = authService.login(LoginCommand("confirm@payvue.com", "new-password"))
          loginResult.accessToken().shouldNotBeBlank()
          sessionTokenRepository.isActive(loginResult.accessToken()) shouldBe true
        }
      }

      `when`("토큰이 유효하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.resetPasswordConfirm("invalid-token", "new-password") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("회원가입 재인증 요청에서") {
      `when`("미인증 사용자일 때") {
        then("인증 토큰이 발급되고 메일이 발송된다") {
          createAndPersistUser("resend@payvue.com", "Passw0rd!")

          service.resendSignupVerification("resend@payvue.com")

          verificationTokenRepository.exists("resend@payvue.com", VerificationPurpose.SIGNUP) shouldBe true
          verify(emailSender).send(anyString(), anyString(), anyString())
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.resendSignupVerification("missing@payvue.com") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("이미 인증된 사용자일 때") {
        then("예외가 발생한다") {
          val user = createAndPersistUser("resend-verified@payvue.com", "Passw0rd!")
          user.verifyEmail()
          userRepository.save(user)

          assertThatThrownBy { service.resendSignupVerification("resend-verified@payvue.com") }
            .isInstanceOf(IllegalStateException::class.java)
        }
      }
    }
  }

  private fun createAndPersistUser(email: String, rawPassword: String): User {
    return createAndPersistUser(email, rawPassword, "login")
  }

  private fun createAndPersistUser(email: String, rawPassword: String, loginId: String): User {
    val encoded = passwordEncoder.encode(rawPassword)
    val user = User.create(
      loginId,
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
      encoded,
      UserPasswordGuard.defaultPolicy(rawPassword)
    )
    return userRepository.save(user)
  }

  @TestConfiguration
  @EnableAutoConfiguration
  @Import(
    VerificationApplicationService::class,
    AuthContextConfiguration::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class,
    AlarmContextConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure.persistence",
      "com.payvue.app.infrastructure.redis",
      "com.payvue.supporting.kyc",
    ],
    excludeFilters = [
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [TestConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [Configuration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RedisRepositoryConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [GmailEmailSender::class])
    ]
  )
  @EntityScan("com.payvue.app.infrastructure.persistence")
  @EnableJpaRepositories("com.payvue.app.infrastructure.persistence")
  open class TestApplication

  @TestConfiguration
  open class TestBeans {
    @Bean
    @Primary
    open fun emailSender(): EmailSender {
      return Mockito.mock(EmailSender::class.java)
    }
  }
}
