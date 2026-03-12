package com.payvue.app.application.asset

import com.payvue.app.domain.asset.category.Cash
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.presentation.rest.hold.AssetOnlineCreateRequest
import com.payvue.characteristic.Consumable
import com.payvue.characteristic.HasBalance
import com.payvue.core.asset.AssetClassification
import com.payvue.core.asset.AssetStatus
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityId
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldNotBeBlank
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
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender

@SpringBootTest(classes = [AssetApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class AssetApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var service: AssetApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository


  init {
    given("온라인 자산 조회 요청에서") {
      `when`("사용자와 개인 커뮤니티에 자산이 있을 때") {
        then("자산 목록이 반환된다") {
          val user = createAndPersistUser("owner@payvue.com")
          val userDetails = mockUserDetails("owner@payvue.com")
          val community = createPersonalCommunity(user)

          val cash = Cash.create(community.getId(), user.getId(), "cash")
            .with(HasBalance.of(Money.won(1200L)))
          val holdings = Holdings.create(community, listOf(cash))
          holdingsRepository.save(holdings)

          val result = service.online(userDetails)

          result.toList() shouldHaveSize 1
          result[0].category() shouldBe AssetClassification.cash().getValue()
          result[0].amount() shouldBe 1200L
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("missing@payvue.com")

          assertThatThrownBy { service.online(userDetails) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("카테고리 기반 온라인 자산 조회 요청에서") {
      `when`("카테고리가 null일 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("owner@payvue.com")

          assertThatThrownBy { service.online(userDetails, null as String?) }
            .isInstanceOf(NullPointerException::class.java)
        }
      }

      `when`("카테고리가 비어 있을 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("owner@payvue.com")

          assertThatThrownBy { service.online(userDetails, " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("사용자와 카테고리에 해당하는 커뮤니티가 있을 때") {
        then("자산 목록이 반환된다") {
          val user = createAndPersistUser("category@payvue.com")
          val userDetails = mockUserDetails("category@payvue.com")
          val community = createPersonalCommunity(user)

          val cash = Cash.create(community.getId(), user.getId(), "cash")
            .with(HasBalance.of(Money.won(800L)))
          val holdings = Holdings.create(community, listOf(cash))
          holdingsRepository.save(holdings)

          val result = service.online(userDetails, CommunityCategory.PERSONAL.name)

          result.toList() shouldHaveSize 1
          result[0].amount() shouldBe 800L
        }
      }
    }

    given("커뮤니티 ID 기반 온라인 자산 조회 요청에서") {
      `when`("커뮤니티 ID가 null일 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("owner@payvue.com")

          assertThatThrownBy { service.online(userDetails, null as CommunityId?) }
            .isInstanceOf(NullPointerException::class.java)
        }
      }

      `when`("커뮤니티 ID가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val user = createAndPersistUser("missing-community@payvue.com")
          val userDetails = mockUserDetails("missing-community@payvue.com")
          createPersonalCommunity(user)

          assertThatThrownBy { service.online(userDetails, CommunityId.generate()) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("커뮤니티 ID가 일치할 때") {
        then("자산 목록이 반환된다") {
          val user = createAndPersistUser("community@payvue.com")
          val userDetails = mockUserDetails("community@payvue.com")
          val community = createPersonalCommunity(user)

          val cash = Cash.create(community.getId(), user.getId(), "cash")
            .with(HasBalance.of(Money.won(500L)))
          val holdings = Holdings.create(community, listOf(cash))
          holdingsRepository.save(holdings)

          val result = service.online(userDetails, community.getId())

          result.toList() shouldHaveSize 1
          result[0].amount() shouldBe 500L
        }
      }
    }

    given("결제 수단 조회 요청에서") {
      `when`("사용자가 존재할 때") {
        then("결제 수단 목록이 반환된다") {
          createAndPersistUser("method@payvue.com")
          val userDetails = mockUserDetails("method@payvue.com")

          val result = service.paymentMethods(userDetails)

          result.toList() shouldHaveSize 2
          result[0].id() shouldBe "pm-001"
        }
      }

      `when`("사용자가 존재하지 않을 때") {
        then("예외가 발생한다") {
          val userDetails = mockUserDetails("missing-method@payvue.com")

          assertThatThrownBy { service.paymentMethods(userDetails) }
            .isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("온라인 자산 생성 요청에서") {
      `when`("요청이 유효할 때") {
        then("자산이 생성되고 보유 목록에 추가된다") {
          val user = createAndPersistUser("create@payvue.com")
          val userDetails = mockUserDetails("create@payvue.com")
          val community = createPersonalCommunity(user)
          holdingsRepository.save(Holdings.create(community, listOf()))

          val assetId = service.createOnlineAsset(
            userDetails,
            AssetOnlineCreateRequest(
              "gift-item",
              "GIFT",
              2L,
              "desc",
              "ACTIVE"
            )
          )

          assetId.shouldNotBeBlank()
          val stored = holdingsRepository.findByCommunity(community)
          stored.getAssets() shouldHaveSize 1
          val item = stored.getAssets()[0]
          item.getCategory().getValue() shouldBe AssetClassification.gift().getValue()
          item.getName() shouldBe "gift-item"
          item.getStatus() shouldBe AssetStatus.ACTIVE
          item.getCapability(Consumable::class.java).orElseThrow().quantity() shouldBe 2
        }
      }

      `when`("status가 INACTIVE일 때") {
        then("자산 상태가 SUSPENDED로 저장된다") {
          val user = createAndPersistUser("inactive@payvue.com")
          val userDetails = mockUserDetails("inactive@payvue.com")
          val community = createPersonalCommunity(user)
          holdingsRepository.save(Holdings.create(community, listOf()))

          service.createOnlineAsset(
            userDetails,
            AssetOnlineCreateRequest(
              "inactive-item",
              "SHOPPING",
              1L,
              "desc",
              "INACTIVE"
            )
          )

          val stored = holdingsRepository.findByCommunity(community)
          stored.getAssets() shouldHaveSize 1
          stored.getAssets()[0].getStatus() shouldBe AssetStatus.SUSPENDED
        }
      }

      `when`("category가 지원되지 않을 때") {
        then("예외가 발생한다") {
          val user = createAndPersistUser("invalid-category@payvue.com")
          val userDetails = mockUserDetails("invalid-category@payvue.com")
          val community = createPersonalCommunity(user)
          holdingsRepository.save(Holdings.create(community, listOf()))

          assertThatThrownBy {
            service.createOnlineAsset(
              userDetails,
              AssetOnlineCreateRequest(
                "invalid",
                "UNKNOWN",
                1L,
                "desc",
                "ACTIVE"
              )
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }
  }

  private fun createPersonalCommunity(user: User): com.payvue.core.community.Community {
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
    AssetApplicationService::class,
    AssetCharacteristicConfiguration::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure.persistence",
      "com.payvue.app.domain.asset",
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
}
