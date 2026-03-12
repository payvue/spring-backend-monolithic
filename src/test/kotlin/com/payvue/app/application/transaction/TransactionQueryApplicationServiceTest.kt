package com.payvue.app.application.transaction

import com.payvue.app.application.transaction.command.TransferCashCommand
import com.payvue.app.domain.asset.category.Cash
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.community.RecipientType
import com.payvue.characteristic.HasBalance
import com.payvue.core.asset.AssetClassification
import com.payvue.core.community.Community
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityInitCommand
import com.payvue.core.hold.Holdings
import com.payvue.core.hold.HoldingsRepository
import com.payvue.core.transaction.command.TransactionType
import com.payvue.generic.alarm.AlarmChannel
import com.payvue.shared.Money
import com.payvue.shared.Period
import com.payvue.supporting.user.UserContextConfiguration
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserAccountView
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import org.springframework.context.annotation.FilterType
import org.springframework.boot.test.context.TestConfiguration
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize

@SpringBootTest(classes = [TransactionQueryApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class TransactionQueryApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var transactionService: TransactionApplicationService

  @Autowired
  private lateinit var queryService: TransactionQueryApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository

  init {
    given("개인 거래 내역 조회 요청에서") {
      `when`("거래가 존재할 때") {
        then("거래 내역과 페이지 정보가 반환된다") {
          val user = createAndPersistUser("history@payvue.com")
          val personal = createPersonalCommunity(user)
          val recipient = createAndPersistUser("history-recipient@payvue.com")
          createPersonalCommunity(recipient)
          saveCashHoldings(personal, user, 800L)

          transactionService.transferCash(
            mockUserDetails("history@payvue.com"),
            TransferCashCommand(
              "history-recipient@payvue.com",
              RecipientType.USER,
              200L,
              "KRW",
              "송금",
              "client-10"
            ),
            "idempotency-10"
          )

          val result = queryService.queryPersonal(
            mockUserDetails("history@payvue.com"),
            AssetClassification.cash().getValue().uppercase(),
            TransactionType.TRANSFER,
            PageRequest.of(0, 10),
            Period.unbounded()
          )

          result.items() shouldHaveSize 1
          result.items()[0].kind() shouldBe "transfer"
          result.items()[0].category() shouldBe AssetClassification.cash().getValue()
          result.pagination().total() shouldBe 1
        }
      }

      `when`("개인 커뮤니티가 존재하지 않을 때") {
        then("예외가 발생한다") {
          createAndPersistUser("no-personal@payvue.com")

          assertThatThrownBy {
            queryService.queryPersonal(
              mockUserDetails("no-personal@payvue.com"),
              AssetClassification.cash().getValue().uppercase(),
              TransactionType.TRANSFER,
              PageRequest.of(0, 10),
              Period.unbounded()
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("커뮤니티 거래 내역 조회 요청에서") {
      `when`("커뮤니티에 거래가 존재할 때") {
        then("커뮤니티 기준 거래 내역이 반환된다") {
          val user = createAndPersistUser("community-history@payvue.com")
          val personal = createPersonalCommunity(user)
          val recipient = createAndPersistUser("community-recipient@payvue.com")
          createPersonalCommunity(recipient)
          saveCashHoldings(personal, user, 900L)

          transactionService.transferCash(
            mockUserDetails("community-history@payvue.com"),
            TransferCashCommand(
              "community-recipient@payvue.com",
              RecipientType.USER,
              100L,
              "KRW",
              "송금",
              "client-11"
            ),
            "idempotency-11"
          )

          val result = queryService.queryCommunity(
            personal.getId(),
            AssetClassification.cash().getValue().uppercase(),
            TransactionType.TRANSFER,
            PageRequest.of(0, 10),
            Period.unbounded()
          )

          result.items() shouldHaveSize 1
          result.items()[0].kind() shouldBe "transfer"
          result.pagination().total() shouldBe 1
        }
      }

      `when`("커뮤니티에 거래가 없을 때") {
        then("빈 목록과 0개의 페이지 정보가 반환된다") {
          val user = createAndPersistUser("empty-community@payvue.com")
          val personal = createPersonalCommunity(user)

          val result = queryService.queryCommunity(
            personal.getId(),
            AssetClassification.cash().getValue().uppercase(),
            TransactionType.TRANSFER,
            PageRequest.of(0, 10),
            Period.unbounded()
          )

          result.items() shouldHaveSize 0
          result.pagination().total() shouldBe 0
        }
      }
    }

    given("개인 거래 내역 조회 요청에서") {
      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy {
            queryService.queryPersonal(
              mockUserDetails("missing@payvue.com"),
              AssetClassification.cash().getValue().uppercase(),
              TransactionType.TRANSFER,
              PageRequest.of(0, 10),
              Period.unbounded()
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }
  }

  private fun createPersonalCommunity(user: User): Community {
    val community = communityContextManager.init(
      CommunityInitCommand(
        CommunityCategory.PERSONAL.name,
        UserAccountView.from(user)
      )
    )
    user.joinCommunity(community.getId().getValue().toString())
    userRepository.save(user)
    return community
  }

  private fun saveCashHoldings(community: Community, user: User, amount: Long) {
    val cash = Cash.create(community.getId(), user.getId(), AssetClassification.cash().getValue())
      .with(HasBalance.of(Money.of(amount, "KRW")))
    val holdings = Holdings.create(community, listOf(cash))
    holdingsRepository.save(holdings)
  }

  private fun createAndPersistUser(email: String): User {
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
      "encoded",
      UserPasswordGuard.defaultPolicy("Passw0rd!")
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
    TransactionApplicationService::class,
    TransactionQueryApplicationService::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.app.domain.asset",
      "com.payvue.core.transaction.command",
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
  open class TestApplication {
    @Bean
    @Primary
    open fun alarmChannel(): AlarmChannel {
      return Mockito.mock(AlarmChannel::class.java)
    }
  }
}
