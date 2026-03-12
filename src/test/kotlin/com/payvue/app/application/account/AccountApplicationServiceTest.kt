package com.payvue.app.application.account

import com.payvue.app.application.account.create.AccountCreateCommand
import com.payvue.app.application.account.edit.AccountEditCommand
import com.payvue.app.application.account.edit.ChangePasswordCommand
import com.payvue.app.application.account.query.AccountExistCommand
import com.payvue.app.domain.auth.AuthContextConfiguration
import com.payvue.app.domain.auth.VerificationPurpose
import com.payvue.app.domain.auth.VerificationTokenRepository
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityInitCommand
import com.payvue.core.community.CommunityContextManager
import com.payvue.shared.Money
import com.payvue.supporting.kyc.UserKycRepository
import com.payvue.supporting.user.UserContextConfiguration
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserAccountView
import com.payvue.supporting.user.command.UserRepository
import com.payvue.app.domain.asset.category.Cash
import com.payvue.app.domain.asset.category.Mileage
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.characteristic.HasBalance
import com.payvue.core.hold.Holdings
import com.payvue.core.hold.HoldingsRepository
import com.payvue.app.infrastructure.mail.EmailSender
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeTrue
import org.awaitility.Awaitility
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.annotation.Rollback
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Duration
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import io.kotest.matchers.shouldBe

@Testcontainers
@SpringBootTest(classes = [AccountApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class AccountApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  companion object {
    @Container
    @JvmStatic
    val redis: GenericContainer<*> = GenericContainer(
      DockerImageName.parse("redis:7-alpine")
    )
      .withExposedPorts(6379)

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
  private lateinit var service: AccountApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var userKycRepository: UserKycRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var verificationTokenRepository: VerificationTokenRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository

  @Autowired
  private lateinit var emailSender: EmailSender

  @Autowired
  private lateinit var redisTemplate: StringRedisTemplate

  init {
    beforeTest {
      Mockito.reset(emailSender)
      redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    given("계정 생성 요청에서") {
      `when`("사용자 정보가 제공될 때") {
        then("사용자가 생성되고 알람 메일이 발송되며 KYC가 저장된다") {
          service.create(accountCreateCommand("881010-1234567"))

          val user = userRepository.findByEmail("user@payvue.com")
          val kyc = userKycRepository.findByUserId(user.get().getId())

          kyc.isPresent.shouldBeTrue()
          user.isPresent.shouldBeTrue()
          Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted {
            Mockito.verify(emailSender).send(anyString(), anyString(), anyString())
          }
          verificationTokenRepository.findByIdentifier(
            "user@payvue.com",
            VerificationPurpose.SIGNUP
          ).isPresent.shouldBeTrue()
        }
      }

      `when`("이미 등록된 이메일이면") {
        then("예외가 발생하고 알람 메일이 발송되지 않는다") {
          createAndPersistUser("user@payvue.com")

          assertThatThrownBy {
            service.create(accountCreateCommand("881010-1234567"))
          }.isInstanceOf(IllegalArgumentException::class.java)

          val user = userRepository.findByEmail("user@payvue.com")
          user.isPresent.shouldBeTrue()
          verificationTokenRepository.findByIdentifier(
            "user@payvue.com",
            VerificationPurpose.SIGNUP
          ).isEmpty.shouldBeTrue()
          Mockito.verify(emailSender, Mockito.never())
            .send(anyString(), anyString(), anyString())
        }
      }

      `when`("주민등록번호가 제공되지 않을 때") {
        then("KYC가 저장되지 않는다") {
          service.create(accountCreateCommand(null))

          val user = userRepository.findByEmail("user@payvue.com").orElseThrow()
          val kyc = userKycRepository.findByUserId(user.getId())

          kyc.isEmpty.shouldBeTrue()
          Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted {
            Mockito.verify(emailSender).send(anyString(), anyString(), anyString())
          }
          verificationTokenRepository.findByIdentifier(
            "user@payvue.com",
            VerificationPurpose.SIGNUP
          ).isPresent.shouldBeTrue()
        }
      }
    }

    given("계정 수정 요청에서") {
      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("missing@payvue.com")

          assertThatThrownBy {
            service.edit(userDetails, AccountEditCommand("new-name", "010-1111-2222"))
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("사용자가 존재할 때") {
        then("실명이 수정된다") {
          createAndPersistUser("edit@payvue.com")
          val userDetails = mockUserDetails("edit@payvue.com")

          service.edit(userDetails, AccountEditCommand("updated-name", "010-1111-2222"))

          val stored = userRepository.findByEmail("edit@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)
          view.realName() shouldBe "updated-name"
        }
      }
    }

    given("비밀번호 변경 요청에서") {
      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("missing@payvue.com")

          assertThatThrownBy {
            service.changePassword(userDetails, ChangePasswordCommand("old", "new-password"))
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("사용자가 존재할 때") {
        then("비밀번호가 변경된다") {
          createAndPersistUser("pw@payvue.com")
          val userDetails = mockUserDetails("pw@payvue.com")

          sessionTokenRepository.save("access-token", "pw@payvue.com", Duration.ofMinutes(5))
          sessionTokenRepository.isActive("access-token") shouldBe true

          service.changePassword(userDetails, ChangePasswordCommand("old", "new-password"))

          sessionTokenRepository.isActive("access-token") shouldBe false
        }
      }
    }

    given("계정 존재 여부 조회 요청에서") {
      `when`("사용자가 존재할 때") {
        then("결과에 이름과 연락처가 포함된다") {
          createAndPersistUser("exist@payvue.com")

          val result = service.exist(AccountExistCommand("exist@payvue.com"))

          val stored = userRepository.findByEmail("exist@payvue.com").orElseThrow()
          val view = UserAccountView.from(stored)
          result.name() shouldBe view.realName()
          result.contact().email() shouldBe "exist@payvue.com"
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("결과가 비어 있다") {
          val result = service.exist(AccountExistCommand("absent@payvue.com"))

          result.name() shouldBe null
          result.contact() shouldBe null
        }
      }
    }

    given("계정 정보 조회 요청에서") {
      `when`("개인 커뮤니티가 존재할 때") {
        then("현금/마일리지 잔액이 반영된다") {
          val user = createAndPersistUser("info@payvue.com")
          user.activate()
          val personal = communityContextManager.init(
            CommunityInitCommand(
              CommunityCategory.PERSONAL.name,
              UserAccountView.from(user)
            )
          )
          user.joinCommunity(personal.getId().getValue().toString())
          userRepository.save(user)

          val cash = Cash.create(personal.getId(), user.getId(), "cash")
              .with(HasBalance.of(Money.won(1200L)))
          val mileage = Mileage.create(personal.getId(), user.getId(), "mileage")
              .with(HasBalance.of(Money.won(300L)))
          val holdings = Holdings.create(personal, listOf(cash, mileage))
          holdingsRepository.save(holdings)

          val result = service.info(mockUserDetails("info@payvue.com"))

          result.asset().balance() shouldBe 1200L
          result.asset().point() shouldBe 300L
        }
      }

      `when`("개인 커뮤니티가 없을 때") {
        then("잔액이 0으로 처리된다") {
          createAndPersistUser("none@payvue.com")

          val result = service.info(mockUserDetails("none@payvue.com"))

          result.asset().balance() shouldBe 0L
          result.asset().point() shouldBe 0L
        }
      }
    }
  }

  private fun accountCreateCommand(residentRegistrationNo: String?): AccountCreateCommand {
    return AccountCreateCommand(
      "alias",
      "login",
      "real-name",
      residentRegistrationNo,
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
      com.payvue.supporting.user.command.UserPasswordGuard.defaultPolicy("Passw0rd!")
    )
  }

  private fun mockUserDetails(email: String): UserDetails {
    val userDetails = org.mockito.Mockito.mock(UserDetails::class.java)
    `when`(userDetails.username).thenReturn(email)
    return userDetails
  }

  private fun createAndPersistUser(email: String): User {
    val user = createUser(email)
    return userRepository.save(user)
  }

  @TestConfiguration
  @EnableAutoConfiguration
  @Import(
    AccountApplicationService::class,
    AuthContextConfiguration::class,
    UserContextConfiguration::class,
    CommunityContextConfiguration::class,
    AssetCharacteristicConfiguration::class,
    TestBeans::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.app.domain.asset",
      "com.payvue.generic.alarm",
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
