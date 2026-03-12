package com.payvue.app.application.auth

import com.payvue.app.application.auth.login.LoginCommand
import com.payvue.app.application.auth.login.LoginResult
import com.payvue.app.application.auth.login.LogoutCommand
import com.payvue.app.application.auth.recovery.FindIdCommand
import com.payvue.app.application.auth.recovery.FindPasswordCommand
import com.payvue.app.domain.auth.AuthContextConfiguration
import com.payvue.app.domain.auth.VerificationPurpose
import com.payvue.app.domain.auth.VerificationTokenRepository
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.app.infrastructure.mail.EmailSender
import com.payvue.app.infrastructure.mail.EmailTemplate
import com.payvue.app.infrastructure.mail.EmailTemplateId
import com.payvue.app.infrastructure.mail.EmailTemplateResolver
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.supporting.user.UserContextConfiguration
import com.payvue.supporting.user.UserContextManager
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.core.userdetails.UserDetails
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

@Testcontainers
@SpringBootTest(classes = [AuthApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class AuthApplicationServiceTest : BehaviorSpec() {

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

  @Qualifier("userContextManager")
  @Autowired
  private lateinit var userContextManager: UserContextManager

  @Autowired
  private lateinit var service: AuthApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var verificationTokenRepository: VerificationTokenRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @Autowired
  private lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  private lateinit var emailSender: EmailSender

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  init {
    beforeTest {
      Mockito.reset(emailSender)
      redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    given("로그인 요청에서") {
      `when`("올바른 비밀번호가 제공될 때") {
        then("세션 토큰이 발급되고 로그인 결과가 반환된다") {
          createAndPersistUser("login@payvue.com", "Passw0rd!")

          val result = service.login(LoginCommand("login@payvue.com", "Passw0rd!"))

          val stored = userRepository.findByEmail("login@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)

          result.accessToken().shouldNotBeBlank()
          sessionTokenRepository.isActive(result.accessToken()) shouldBe true
          view.loggedIn() shouldBe true
        }
      }

      `when`("비밀번호가 일치하지 않을 때") {
        then("예외가 발생한다") {
          createAndPersistUser("wrong@payvue.com", "Passw0rd!")

          assertThatThrownBy { service.login(LoginCommand("wrong@payvue.com", "invalid")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.login(LoginCommand("missing@payvue.com", "Passw0rd!")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("로그아웃 요청에서") {
      `when`("사용자가 존재할 때") {
        then("로그인 상태가 해제된다") {
          createAndPersistUser("logout@payvue.com", "Passw0rd!")
          val result = service.login(LoginCommand("logout@payvue.com", "Passw0rd!"))

          service.logout(mockUserDetails("logout@payvue.com"), LogoutCommand("token"))

          val stored = userRepository.findByEmail("logout@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)
          view.loggedIn() shouldBe false
          sessionTokenRepository.isActive(result.accessToken) shouldBe false
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy {
            service.logout(mockUserDetails("missing@payvue.com"), LogoutCommand("token"))
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("아이디 찾기 요청에서") {
      `when`("사용자가 존재할 때") {
        then("인증 토큰이 발급되고 메일이 발송된다") {
          createAndPersistUser("find@payvue.com", "Passw0rd!")

          service.findId(FindIdCommand("find@payvue.com"))

          verificationTokenRepository.exists("find@payvue.com", VerificationPurpose.FIND_ID) shouldBe true
          verify(emailSender).send(anyString(), anyString(), anyString())
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.findId(FindIdCommand("missing@payvue.com")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("비밀번호 찾기 요청에서") {
      `when`("사용자가 존재할 때") {
        then("인증 토큰이 발급되고 메일이 발송된다") {
          createAndPersistUser("reset@payvue.com", "Passw0rd!")

          service.findPassword(FindPasswordCommand("login", "reset@payvue.com"))

          verificationTokenRepository.exists("reset@payvue.com", VerificationPurpose.RESET_PASSWORD) shouldBe true
          verify(emailSender).send(anyString(), anyString(), anyString())
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy { service.findPassword(FindPasswordCommand("login", "missing@payvue.com")) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }
  }

  private fun createAndPersistUser(email: String, rawPassword: String): User {
    val encoded = passwordEncoder.encode(rawPassword)
    val user = User.create(
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
      encoded,
      UserPasswordGuard.defaultPolicy(rawPassword)
    )
    return userRepository.save(user)
  }

  private fun mockUserDetails(email: String): UserDetails {
    val userDetails = Mockito.mock(UserDetails::class.java)
    `when`(userDetails.username).thenReturn(email)
    return userDetails
  }

  @TestConfiguration
  @EnableAutoConfiguration
  @Import(
    AuthApplicationService::class,
    AuthContextConfiguration::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class,
    TestBeans::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.supporting.kyc",
    ],
    excludeFilters = [
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [TestConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [Configuration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RedisRepositoryConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [GmailEmailSender::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [PgClientConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [NaverPaymentGateway::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [VirtualAccountTossGateway::class])
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
