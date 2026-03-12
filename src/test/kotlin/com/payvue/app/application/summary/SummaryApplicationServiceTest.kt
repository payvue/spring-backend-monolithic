package com.payvue.app.application.summary

import com.payvue.app.domain.asset.category.Cash
import com.payvue.app.domain.asset.category.GiftCard
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.summary.BalancesCategoryingAggregationStrategy
import com.payvue.app.domain.summary.SummaryServiceImpl
import com.payvue.characteristic.HasBalance
import com.payvue.core.asset.AssetClassification
import com.payvue.core.community.Community
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityInitCommand
import com.payvue.core.hold.Holdings
import com.payvue.core.hold.HoldingsRepository
import com.payvue.shared.Money
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
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import org.springframework.context.annotation.FilterType
import org.springframework.boot.test.context.TestConfiguration
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize

@SpringBootTest(classes = [SummaryApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class SummaryApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var service: SummaryApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository

  init {
    given("요약 집계 요청에서") {
      `when`("개인/공유 커뮤니티에 자산이 있을 때") {
        then("personal/shared 구분된 합산 결과가 반환된다") {
          val user = createAndPersistUser("summary@payvue.com")
          val personal = createPersonalCommunity(user)
          val shared = createCooperativeCommunity(user)

          saveHoldings(
            personal,
            user,
            cashAmount = 1000L,
            giftAmount = 200L
          )
          saveHoldings(
            shared,
            user,
            cashAmount = 400L,
            giftAmount = 300L
          )

          val result = service.accumulate(
            mockUserDetails("summary@payvue.com"),
            setOf(AssetClassification.gift())
          )

          result.totals().cash() shouldBe 1400L
          result.totals().assetValue() shouldBe 1900L
          result.personal().cash() shouldBe 1000L
          result.personal().assetValue() shouldBe 1200L
          result.shared().cash() shouldBe 400L
          result.shared().assetValue() shouldBe 700L
          result.categories() shouldHaveSize 1
          result.categories()[0].category() shouldBe AssetClassification.gift().getValue()
          result.categories()[0].amount() shouldBe 500L
        }
      }

      `when`("카테고리 집합이 비어 있을 때") {
        then("기본 카테고리로 집계된다") {
          val user = createAndPersistUser("default@payvue.com")
          val personal = createPersonalCommunity(user)
          saveHoldings(
            personal,
            user,
            cashAmount = 300L,
            giftAmount = 500L
          )

          val result = service.accumulate(
            mockUserDetails("default@payvue.com"),
            emptySet()
          )

          result.categories() shouldHaveSize 1
          result.categories()[0].category() shouldBe AssetClassification.gift().getValue()
          result.personal().assetValue() shouldBe 800L
          result.shared().assetValue() shouldBe 0L
        }
      }

      `when`("개인 커뮤니티만 존재할 때") {
        then("shared 합산은 0으로 처리된다") {
          val user = createAndPersistUser("only-personal@payvue.com")
          val personal = createPersonalCommunity(user)
          saveHoldings(
            personal,
            user,
            cashAmount = 600L,
            giftAmount = 400L
          )

          val result = service.accumulate(
            mockUserDetails("only-personal@payvue.com"),
            setOf(AssetClassification.gift())
          )

          result.personal().assetValue() shouldBe 1000L
          result.shared().assetValue() shouldBe 0L
          result.totals().assetValue() shouldBe 1000L
        }
      }

      `when`("카테고리 필터가 현금만 포함할 때") {
        then("카테고리 합산에는 현금만 반영된다") {
          val user = createAndPersistUser("cash-only@payvue.com")
          val personal = createPersonalCommunity(user)
          saveHoldings(
            personal,
            user,
            cashAmount = 700L,
            giftAmount = 300L
          )

          val result = service.accumulate(
            mockUserDetails("cash-only@payvue.com"),
            setOf(AssetClassification.cash())
          )

          result.categories() shouldHaveSize 1
          result.categories()[0].category() shouldBe AssetClassification.cash().getValue()
          result.categories()[0].amount() shouldBe 700L
          result.totals().assetValue() shouldBe 1000L
        }
      }

      `when`("categories가 null일 때") {
        then("예외가 발생한다") {
          val user = createAndPersistUser("null-category@payvue.com")
          createPersonalCommunity(user)

          assertThatThrownBy {
            service.accumulate(
              mockUserDetails("null-category@payvue.com"),
              null as Set<AssetClassification>?
            )
          }.isInstanceOf(NullPointerException::class.java)
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy {
            service.accumulate(
              mockUserDetails("missing@payvue.com"),
              setOf(AssetClassification.cash())
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

  private fun createCooperativeCommunity(user: User): Community {
    val community = communityContextManager.init(
      CommunityInitCommand(
        CommunityCategory.COOPERATIVE.name,
        UserAccountView.from(user)
      )
    )
    user.joinCommunity(community.getId().getValue().toString())
    userRepository.save(user)
    return community
  }

  private fun saveHoldings(
    community: Community,
    user: User,
    cashAmount: Long,
    giftAmount: Long
  ) {
    val cash = Cash.create(community.getId(), user.getId(), "cash")
      .with(HasBalance.of(Money.of(cashAmount, "KRW")))
    val gift = GiftCard.create(community.getId(), user.getId(), "gift")
      .with(HasBalance.of(Money.of(giftAmount, "KRW")))
    val holdings = Holdings.create(community, listOf(cash, gift))
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
    SummaryApplicationService::class,
    SummaryServiceImpl::class,
    BalancesCategoryingAggregationStrategy::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class
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
}
