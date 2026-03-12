package com.payvue.app.application.community

import com.payvue.app.application.community.create.CommunityCreateCommand
import com.payvue.app.domain.asset.OnlineCashManager
import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.community.Cooperative
import com.payvue.app.domain.community.RoleCatalog
import com.payvue.common.status.EngagementState
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration
import com.payvue.core.community.Community
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityInitCommand
import com.payvue.core.community.CommunityRepository
import com.payvue.core.hold.HoldingsRepository
import com.payvue.supporting.interaction.InteractionConfiguration
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
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import org.springframework.context.annotation.FilterType
import org.springframework.boot.test.context.TestConfiguration
import com.payvue.app.infrastructure.redis.RedisRepositoryConfiguration
import com.payvue.app.infrastructure.pg.PgClientConfiguration
import com.payvue.app.infrastructure.pg.naver.NaverPaymentGateway
import com.payvue.app.infrastructure.pg.toss.VirtualAccountTossGateway
import com.payvue.app.infrastructure.redis.verification.RedisVerificationTokenRepository
import com.payvue.app.infrastructure.security.JwtProvider
import org.springframework.context.annotation.Configuration
import com.payvue.app.infrastructure.mail.GmailEmailSender
import jakarta.persistence.EntityManager
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank

@SpringBootTest(classes = [CommunityApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class CommunityApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var service: CommunityApplicationService

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var communityRepository: CommunityRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var roleCatalog: RoleCatalog

  @Autowired
  private lateinit var onlineCashManager: OnlineCashManager

  @Autowired
  private lateinit var holdingsRepository: HoldingsRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  private lateinit var entityManager: EntityManager

  init {
    beforeTest {
      transactionTemplate.execute {
        entityManager.createQuery("delete from ParticipantTBL").executeUpdate()
        entityManager.createQuery("delete from PolicyRuleTBL").executeUpdate()
        entityManager.createQuery("delete from PersonalTBL").executeUpdate()
        entityManager.createQuery("delete from CooperativeTBL").executeUpdate()
        entityManager.createQuery("delete from CommunityTBL").executeUpdate()
        entityManager.createQuery("delete from PaymentRequestEntity").executeUpdate()
        entityManager.createQuery("delete from UserMembershipEntity").executeUpdate()
        entityManager.createQuery("delete from UserPasswordHistoryEntity").executeUpdate()
        entityManager.createQuery("delete from UserFeaturePreferenceEntity").executeUpdate()
        entityManager.createQuery("delete from UserKycEntity").executeUpdate()
        entityManager.createQuery("delete from UserEntity").executeUpdate()
      }
    }

    given("참여 조회 요청에서") {
      `when`("개인 커뮤니티가 존재할 때") {
        then("참여 중인 커뮤니티 목록이 반환된다") {
          val user = createAndPersistUser("participant@payvue.com")
          createPersonalCommunity(user)

          val result = service.participating(
            mockUserDetails("participant@payvue.com"),
            true
          )

          result.groups() shouldHaveSize 1
          result.groups()[0].name().shouldNotBeBlank()
        }
      }

      `when`("참여 중인 커뮤니티가 없을 때") {
        then("빈 목록이 반환된다") {
          createAndPersistUser("none@payvue.com")

          val result = service.participating(
            mockUserDetails("none@payvue.com"),
            true
          )

          result.groups().shouldBeEmpty()
        }
      }

      `when`("개인/조직 커뮤니티가 모두 존재하고 standAlone이 true일 때") {
        then("개인 커뮤니티만 반환된다") {
          val user = createAndPersistUser("standalone@payvue.com")
          val personal = createPersonalCommunity(user)
          createCooperativeCommunity(user)

          val result = service.participating(
            mockUserDetails("standalone@payvue.com"),
            true
          )

          result.groups() shouldHaveSize 1
          result.groups()[0].id() shouldBe personal.getId().toString()
        }
      }

      `when`("개인/조직 커뮤니티가 모두 존재하고 standAlone이 false일 때") {
        then("조직 커뮤니티만 반환된다") {
          val user = createAndPersistUser("group-only@payvue.com")
          createPersonalCommunity(user)
          val cooperative = createCooperativeCommunity(user)

          val result = service.participating(
            mockUserDetails("group-only@payvue.com"),
            false
          )

          result.groups() shouldHaveSize 1
          result.groups()[0].id() shouldBe cooperative.getId().toString()
        }
      }
    }

    given("커뮤니티 생성 요청에서") {
      `when`("standAlone이 true일 때") {
        then("예외가 발생한다") {
          val owner = createAndPersistUser("standalone-create@payvue.com")
          createPersonalCommunity(owner)

          assertThatThrownBy {
            service.create(
              mockUserDetails("standalone-create@payvue.com"),
              true,
              CommunityCreateCommand(
                "개인 커뮤니티",
                "standalone",
                0L,
                null
              )
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("개인 커뮤니티 잔액이 충분할 때") {
        then("조직 커뮤니티가 생성되고 초기 자산이 반영된다") {
          val owner = createAndPersistUser("owner@payvue.com")
          val personal = createPersonalCommunity(owner)
          onlineCashManager.initial(personal, owner, 1000L)

          service.create(
            mockUserDetails("owner@payvue.com"),
            false,
            CommunityCreateCommand(
              "페이뷰 팀",
              "조직 커뮤니티",
              500L,
              listOf("missing@payvue.com")
            )
          )

          communityRepository.existsByName("페이뷰 팀") shouldBe true
          val refreshed = userRepository.findByEmail("owner@payvue.com").orElseThrow()
          val communities = communityContextManager.resolve(
            refreshed,
            CommunityCategory.COOPERATIVE.name
          )
          communities shouldHaveSize 1
          val created = communities.iterator().next()
          onlineCashManager.possesses(created).toLong() shouldBe 500L
        }
      }

      `when`("개인 커뮤니티 잔액이 부족할 때") {
        then("예외가 발생한다") {
          val owner = createAndPersistUser("insufficient@payvue.com")
          val personal = createPersonalCommunity(owner)
          onlineCashManager.initial(personal, owner, 100L)

          assertThatThrownBy {
            service.create(
              mockUserDetails("insufficient@payvue.com"),
              false,
              CommunityCreateCommand(
                "잔액부족",
                "조직 커뮤니티",
                500L,
                null
              )
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }

      `when`("이미 동일한 이름의 커뮤니티가 존재할 때") {
        then("예외가 발생한다") {
          val owner = createAndPersistUser("duplicate@payvue.com")
          val personal = createPersonalCommunity(owner)
          onlineCashManager.initial(personal, owner, 1000L)
          communityRepository.save(
            Cooperative.create(
              "중복 커뮤니티",
              "조직 커뮤니티",
              owner.getId().getValue(),
              roleCatalog.admin(),
              roleCatalog.member()
            )
          )

          assertThatThrownBy {
            service.create(
              mockUserDetails("duplicate@payvue.com"),
              false,
              CommunityCreateCommand(
                "중복 커뮤니티",
                "조직 커뮤니티",
                100L,
                null
              )
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }

    given("커뮤니티 초대 요청에서") {
      `when`("초대 대상이 존재할 때") {
        then("참여자가 추가된다") {
          val initiator = createAndPersistUser("inviter@payvue.com")
          val invitee = createAndPersistUser("invitee@payvue.com")
          val community = createCooperativeCommunity(initiator)

          service.invite(
            mockUserDetails("inviter@payvue.com"),
            community.getId().getValue(),
            listOf("invitee@payvue.com")
          )

          val stored = communityRepository.findById(community.getId()).orElseThrow()
          stored.getParticipantCount() shouldBe 2
          stored.isParticipant(invitee.getId().getValue()) shouldBe true
          stored.getParticipantState(invitee.getId().value) shouldBe EngagementState.PENDING
        }
      }

      `when`("초대 대상이 존재하지 않을 때") {
        then("예외 없이 처리되고 참여자 수가 유지된다") {
          val initiator = createAndPersistUser("no-invite@payvue.com")
          val community = createCooperativeCommunity(initiator)

          service.invite(
            mockUserDetails("no-invite@payvue.com"),
            community.getId().getValue(),
            listOf("missing@payvue.com")
          )

          val stored = communityRepository.findById(community.getId()).orElseThrow()
          stored.getParticipantCount() shouldBe 1
        }
      }

      `when`("초대 대상 목록이 비어 있을 때") {
        then("예외 없이 처리되고 참여자 수가 유지된다") {
          val initiator = createAndPersistUser("empty-invite@payvue.com")
          val community = createCooperativeCommunity(initiator)

          service.invite(
            mockUserDetails("empty-invite@payvue.com"),
            community.getId().getValue(),
            listOf()
          )

          val stored = communityRepository.findById(community.getId()).orElseThrow()
          stored.getParticipantCount() shouldBe 1
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
    holdingsRepository.findByCommunity(community)
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
    CommunityApplicationService::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class,
    InteractionConfiguration::class,
    AssetCharacteristicConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.supporting.user",
      "com.payvue.supporting.kyc",
    ],
    excludeFilters = [
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [TestConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [Configuration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RedisRepositoryConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [GmailEmailSender::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [PgClientConfiguration::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [NaverPaymentGateway::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [VirtualAccountTossGateway::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RedisVerificationTokenRepository::class]),
      ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [JwtProvider::class]),
      ComponentScan.Filter(type = FilterType.REGEX, pattern = ["com\\.payvue\\.app\\.infrastructure\\.security\\..*"])
    ]
  )
  @EntityScan("com.payvue.app.infrastructure.persistence")
  @EnableJpaRepositories("com.payvue.app.infrastructure.persistence")
  open class TestApplication
}
