package com.payvue.app.application.transaction

import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand
import com.payvue.app.application.transaction.command.TransferAssetToUserCommand
import com.payvue.app.application.transaction.command.TransferCashCommand
import com.payvue.app.domain.asset.category.Cash
import com.payvue.app.domain.asset.category.Coupon
import com.payvue.app.domain.asset.category.GiftCard
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.community.Cooperative
import com.payvue.app.domain.community.RecipientType
import com.payvue.characteristic.Consumable
import com.payvue.characteristic.HasBalance
import com.payvue.core.asset.AssetClassification
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration
import com.payvue.core.community.Community
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityInitCommand
import com.payvue.core.community.CommunityRepository
import com.payvue.core.hold.Holdings
import com.payvue.core.hold.HoldingsRepository
import com.payvue.core.transaction.command.AssetType
import com.payvue.core.transaction.command.TransactionId
import com.payvue.core.transaction.command.TransactionRepository
import com.payvue.generic.alarm.Alarm
import com.payvue.generic.alarm.AlarmChannel
import com.payvue.generic.alarm.AlarmContextConfiguration
import com.payvue.shared.Money
import com.payvue.supporting.interaction.InteractionConfiguration
import com.payvue.supporting.user.UserContextConfiguration
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserAccountView
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import io.kotest.matchers.string.shouldNotBeBlank
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import org.springframework.context.annotation.FilterType
import org.springframework.boot.test.context.TestConfiguration
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import io.kotest.matchers.shouldBe

@SpringBootTest(classes = [TransactionApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class TransactionApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var service: TransactionApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var communityRepository: CommunityRepository

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Autowired
  private lateinit var alarmChannel: AlarmChannel

  init {
    beforeTest {
      Mockito.reset(alarmChannel)
    }

    given("현금 송금 요청에서") {
      `when`("개인 커뮤니티와 자산이 존재할 때") {
        then("송신자 잔액이 감소하고 수신자 잔액이 증가한다") {
          val sender = createAndPersistUser("sender@payvue.com")
          val personal = createPersonalCommunity(sender)
          val recipient = createAndPersistUser("recipient@payvue.com")
          val recipientCommunity = createPersonalCommunity(recipient)
          saveCashHoldings(personal, sender, 1000L)
          val senderBefore = cashBalance(personal)
          val recipientBefore = cashBalance(recipientCommunity)

          val result = service.transferCash(
            mockUserDetails("sender@payvue.com"),
            TransferCashCommand(
              "recipient@payvue.com",
              RecipientType.USER,
              300L,
              "KRW",
              "송금",
              "client-1"
            ),
            "idempotency-1"
          )

          result.balance() shouldBe 300L
          result.transferId().shouldNotBeBlank()
          cashBalance(personal) shouldBe senderBefore - 300L
          cashBalance(recipientCommunity) shouldBe recipientBefore + 300L
          verify(alarmChannel).send(any(Alarm::class.java))
        }
      }

      `when`("수신 커뮤니티가 조직일 때") {
        then("송신자 잔액이 감소하고 조직 잔액이 증가한다") {
          val sender = createAndPersistUser("org-sender@payvue.com")
          val personal = createPersonalCommunity(sender)
          val member = createAndPersistUser("org-member@payvue.com")
          val cooperative = createCooperativeCommunity(member)
          saveCashHoldings(personal, sender, 800L)
          val senderBefore = cashBalance(personal)
          val recipientBefore = cashBalance(cooperative)

          service.transferCash(
            mockUserDetails("org-sender@payvue.com"),
            TransferCashCommand(
              cooperative.getId().getValue().toString(),
              RecipientType.COMMUNITY,
              200L,
              "KRW",
              "송금",
              "client-1-1"
            ),
            "idempotency-1-1"
          )

          cashBalance(personal) shouldBe senderBefore - 200L
          cashBalance(cooperative) shouldBe recipientBefore + 200L
        }
      }

      `when`("수신자가 사용자 ID로 제공될 때") {
        then("송신자 잔액이 감소하고 수신자 잔액이 증가한다") {
          val sender = createAndPersistUser("id-sender@payvue.com")
          val personal = createPersonalCommunity(sender)
          val recipient = createAndPersistUser("id-recipient@payvue.com")
          val recipientCommunity = createPersonalCommunity(recipient)
          saveCashHoldings(personal, sender, 600L)
          val senderBefore = cashBalance(personal)
          val recipientBefore = cashBalance(recipientCommunity)

          service.transferCash(
            mockUserDetails("id-sender@payvue.com"),
            TransferCashCommand(
              recipient.getId().getValue().toString(),
              RecipientType.USER,
              150L,
              "KRW",
              "송금",
              "client-id-1"
            ),
            "idempotency-id-1"
          )

          cashBalance(personal) shouldBe senderBefore - 150L
          cashBalance(recipientCommunity) shouldBe recipientBefore + 150L
        }
      }

      `when`("송금 금액이 잔액과 동일할 때") {
        then("예외가 발생하고 잔액이 유지된다") {
          val sender = createAndPersistUser("equal@payvue.com")
          val personal = createPersonalCommunity(sender)
          val recipient = createAndPersistUser("equal-recipient@payvue.com")
          val recipientCommunity = createPersonalCommunity(recipient)
          saveCashHoldings(personal, sender, 500L)
          val senderBefore = cashBalance(personal)
          val recipientBefore = cashBalance(recipientCommunity)

          assertThatThrownBy {
            service.transferCash(
              mockUserDetails("equal@payvue.com"),
              TransferCashCommand(
                "equal-recipient@payvue.com",
                RecipientType.USER,
                500L,
                "KRW",
                "송금",
                "client-1-2"
              ),
              "idempotency-1-2"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)

          cashBalance(personal) shouldBe senderBefore
          cashBalance(recipientCommunity) shouldBe recipientBefore
        }
      }

      `when`("송금 금액이 잔액보다 클 때") {
        then("예외가 발생하고 잔액이 유지된다") {
          val sender = createAndPersistUser("short@payvue.com")
          val personal = createPersonalCommunity(sender)
          val recipient = createAndPersistUser("short-recipient@payvue.com")
          val recipientCommunity = createPersonalCommunity(recipient)
          saveCashHoldings(personal, sender, 300L)
          val senderBefore = cashBalance(personal)
          val recipientBefore = cashBalance(recipientCommunity)

          assertThatThrownBy {
            service.transferCash(
              mockUserDetails("short@payvue.com"),
              TransferCashCommand(
                "short-recipient@payvue.com",
                RecipientType.USER,
                500L,
                "KRW",
                "송금",
                "client-1-3"
              ),
              "idempotency-1-3"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)

          cashBalance(personal) shouldBe senderBefore
          cashBalance(recipientCommunity) shouldBe recipientBefore
          Mockito.verify(alarmChannel, Mockito.never()).send(any(Alarm::class.java))
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy {
            service.transferCash(
              mockUserDetails("missing@payvue.com"),
              TransferCashCommand(
                "missing-recipient@payvue.com",
                RecipientType.USER,
                300L,
                "KRW",
                "송금",
                "client-1"
              ),
              "idempotency-2"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("커뮤니티 ID 형식이 올바르지 않을 때") {
        then("예외가 발생한다") {
          val sender = createAndPersistUser("invalid-community@payvue.com")
          createPersonalCommunity(sender)

          assertThatThrownBy {
            service.transferCash(
              mockUserDetails("invalid-community@payvue.com"),
              TransferCashCommand(
                "not-a-uuid",
                RecipientType.COMMUNITY,
                100L,
                "KRW",
                "송금",
                "client-invalid"
              ),
              "idempotency-invalid"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("사용자 간 자산 이전 요청에서") {
      `when`("송신자와 수신자가 존재할 때") {
        then("송신자 수량이 감소하고 수신자 수량이 증가한다") {
          val sender = createAndPersistUser("asset@payvue.com")
          val recipient = createAndPersistUser("recipient@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val recipientPersonal = createPersonalCommunity(recipient)
          val coupon = saveCouponHoldings(senderPersonal, sender, 1)
          val senderBefore = consumableQuantity(senderPersonal, AssetClassification.coupon())
          val recipientBefore = consumableQuantity(recipientPersonal, AssetClassification.coupon())

          val result = service.transferAssetToUser(
            mockUserDetails("asset@payvue.com"),
            AssetType.COUPON,
            TransferAssetToUserCommand(coupon.getId().toString(), "recipient@payvue.com", "이체", "client-2"),
            "idempotency-3"
          )

          result.transferId().shouldNotBeBlank()
          consumableQuantity(senderPersonal, AssetClassification.coupon()) shouldBe senderBefore - 1
          consumableQuantity(recipientPersonal, AssetClassification.coupon()) shouldBe recipientBefore + 1
          verify(alarmChannel).send(any(Alarm::class.java))
        }
      }

      `when`("쿠폰 자산을 이전할 때") {
        then("송신자 수량이 감소하고 수신자 수량이 증가한다") {
          val sender = createAndPersistUser("coupon@payvue.com")
          val recipient = createAndPersistUser("coupon-recipient@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val recipientPersonal = createPersonalCommunity(recipient)
          val coupon = saveCouponHoldings(senderPersonal, sender, 1)
          val senderBefore = consumableQuantity(senderPersonal, AssetClassification.coupon())
          val recipientBefore = consumableQuantity(recipientPersonal, AssetClassification.coupon())

          service.transferAssetToUser(
            mockUserDetails("coupon@payvue.com"),
            AssetType.COUPON,
            TransferAssetToUserCommand(coupon.getId().toString(), "coupon-recipient@payvue.com", "이체", "client-2-2"),
            "idempotency-3-2"
          )

          consumableQuantity(senderPersonal, AssetClassification.coupon()) shouldBe senderBefore - 1
          consumableQuantity(recipientPersonal, AssetClassification.coupon()) shouldBe recipientBefore + 1
          verify(alarmChannel).send(any(Alarm::class.java))
        }
      }

      `when`("기프트카드 자산을 이전할 때") {
        then("예외가 발생하고 수량이 유지된다") {
          val sender = createAndPersistUser("gift@payvue.com")
          val recipient = createAndPersistUser("gift-recipient@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val recipientPersonal = createPersonalCommunity(recipient)
          val gift = saveGiftHoldings(senderPersonal, sender, 1)
          val senderBefore = consumableQuantity(senderPersonal, AssetClassification.gift())
          val recipientBefore = consumableQuantity(recipientPersonal, AssetClassification.gift())

          assertThatThrownBy {
            service.transferAssetToUser(
              mockUserDetails("gift@payvue.com"),
              AssetType.GIFT_CARD,
              TransferAssetToUserCommand(gift.getId().toString(), "gift-recipient@payvue.com", "이체", "client-2-3"),
              "idempotency-3-3"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)

          consumableQuantity(senderPersonal, AssetClassification.gift()) shouldBe senderBefore
          consumableQuantity(recipientPersonal, AssetClassification.gift()) shouldBe recipientBefore
          Mockito.verify(alarmChannel, Mockito.never()).send(any(Alarm::class.java))
        }
      }

      `when`("송신자가 자산을 보유하지 않을 때") {
        then("예외가 발생한다") {
          val sender = createAndPersistUser("asset-empty@payvue.com")
          val recipient = createAndPersistUser("recipient-empty@payvue.com")
          createPersonalCommunity(sender)
          createPersonalCommunity(recipient)

          assertThatThrownBy {
            service.transferAssetToUser(
              mockUserDetails("asset-empty@payvue.com"),
              AssetType.CASH,
              TransferAssetToUserCommand(UUID.randomUUID().toString(), "recipient-empty@payvue.com", "이체", "client-2-1"),
              "idempotency-3-1"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("수신자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val sender = createAndPersistUser("asset-missing@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val cash = saveCashHoldings(senderPersonal, sender, 700L)

          assertThatThrownBy {
            service.transferAssetToUser(
              mockUserDetails("asset-missing@payvue.com"),
              AssetType.CASH,
              TransferAssetToUserCommand(cash.getId().toString(), "missing@payvue.com", "이체", "client-3"),
              "idempotency-4"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("커뮤니티로 자산 이전 요청에서") {
      `when`("대상 커뮤니티가 존재할 때") {
        then("송신자 수량이 감소하고 커뮤니티 수량이 증가한다") {
          val sender = createAndPersistUser("group@payvue.com")
          val participant = createAndPersistUser("member@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val target = createCooperativeCommunity(sender)
          target.addMember(participant.getId().getValue()) { true }
          communityRepository.save(target)
          val coupon = saveCouponHoldings(senderPersonal, sender, 1)
          val senderBefore = consumableQuantity(senderPersonal, AssetClassification.coupon())
          val targetBefore = consumableQuantity(target, AssetClassification.coupon())

          val result = service.transferAssetToCommunity(
            mockUserDetails("group@payvue.com"),
            AssetType.COUPON,
            TransferAssetToCommunityCommand(coupon.getId().toString(), target.getId().toString(), "이체", "client-4"),
            "idempotency-5"
          )

          result.transferId().shouldNotBeBlank()
          consumableQuantity(senderPersonal, AssetClassification.coupon()) shouldBe senderBefore - 1
          consumableQuantity(target, AssetClassification.coupon()) shouldBe targetBefore + 1
          verify(alarmChannel).send(any(Alarm::class.java))
        }
      }

      `when`("송신자가 자산을 보유하지 않을 때") {
        then("예외가 발생한다") {
          val sender = createAndPersistUser("group-empty@payvue.com")
          val participant = createAndPersistUser("member-empty@payvue.com")
          createPersonalCommunity(sender)
          val target = createCooperativeCommunity(participant)

          assertThatThrownBy {
            service.transferAssetToCommunity(
              mockUserDetails("group-empty@payvue.com"),
              AssetType.CASH,
              TransferAssetToCommunityCommand(UUID.randomUUID().toString(), target.getId().toString(), "이체", "client-4-1"),
              "idempotency-5-1"
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("대상 커뮤니티가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val sender = createAndPersistUser("group-missing@payvue.com")
          val senderPersonal = createPersonalCommunity(sender)
          val cash = saveCashHoldings(senderPersonal, sender, 500L)

          assertThatThrownBy {
            service.transferAssetToCommunity(
              mockUserDetails("group-missing@payvue.com"),
              AssetType.CASH,
              TransferAssetToCommunityCommand(cash.getId().toString(), UUID.randomUUID().toString(), "이체", "client-5"),
              "idempotency-6"
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

  private fun createCooperativeCommunity(user: User): Cooperative {
    val community = communityContextManager.init(
      CommunityInitCommand(
        CommunityCategory.COOPERATIVE.name,
        UserAccountView.from(user)
      )
    )
    user.joinCommunity(community.getId().getValue().toString())
    userRepository.save(user)
    if (community is Cooperative) {
      return community
    }
    throw IllegalStateException("조직 커뮤니티를 생성할 수 없습니다.")
  }

  private fun saveCashHoldings(community: Community, user: User, amount: Long): Cash {
    val cash = Cash.create(community.getId(), user.getId(), AssetClassification.cash().getValue())
      .with(HasBalance.of(Money.of(amount, "KRW")))
    val holdings = Holdings.create(community, listOf(cash))
    holdingsRepository.save(holdings)
    return cash
  }

  private fun saveCouponHoldings(community: Community, user: User, quantity: Int): Coupon {
    val coupon = Coupon.create(community.getId(), user.getId(), "coupon")
      .with(Consumable.singleUse(quantity))
    val holdings = Holdings.create(community, listOf(coupon))
    holdingsRepository.save(holdings)
    return coupon
  }

  private fun saveGiftHoldings(community: Community, user: User, quantity: Int): GiftCard {
    val gift = GiftCard.create(community.getId(), user.getId(), "gift")
      .with(Consumable.singleUse(quantity))
    val holdings = Holdings.create(community, listOf(gift))
    holdingsRepository.save(holdings)
    return gift
  }

  private fun cashBalance(community: Community): Long {
    val holdings = holdingsRepository.findByCommunity(community)
    val cash = holdings.assets.firstOrNull { item ->
      item != null && item.category == AssetClassification.cash()
    } ?: return 0L
    val balance = cash.getCapability(HasBalance::class.java).orElse(null) ?: return 0L
    return balance.balance().toLong()
  }

  private fun consumableQuantity(community: Community, classification: AssetClassification): Int {
    val holdings = holdingsRepository.findByCommunity(community)
    val asset = holdings.assets.firstOrNull { item ->
      item != null && item.category == classification
    } ?: return 0
    val consumable = asset.getCapability(Consumable::class.java).orElse(null) ?: return 0
    return consumable.quantity()
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
    CommunityContextConfiguration::class,
    UserContextConfiguration::class,
    InteractionConfiguration::class,
    AssetCharacteristicConfiguration::class,
    AlarmContextConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.app.domain.asset",
      "com.payvue.app.domain.community",
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
