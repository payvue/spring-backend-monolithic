package com.payvue.app.application.community

import com.payvue.app.domain.community.CommunityCategory
import com.payvue.app.domain.paymentrequest.PaymentRequestId
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository
import com.payvue.app.presentation.rest.community.PaymentRequestApproveRequest
import com.payvue.app.presentation.rest.community.PaymentRequestCreateRequest
import com.payvue.app.presentation.rest.community.PaymentRequestRejectRequest
import com.payvue.app.domain.asset.OnlineCashManager
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration
import com.payvue.core.community.Community
import com.payvue.core.community.CommunityContextConfiguration
import com.payvue.core.community.CommunityContextManager
import com.payvue.core.community.CommunityInitCommand
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
import java.util.UUID
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe

@SpringBootTest(classes = [PaymentRequestApplicationServiceTest.TestApplication::class])
@ActiveProfiles("test")
@Transactional
@Rollback
open class PaymentRequestApplicationServiceTest : BehaviorSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var service: PaymentRequestApplicationService

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var communityContextManager: CommunityContextManager

  @Autowired
  private lateinit var paymentRequestRepository: PaymentRequestRepository

  @Autowired
  private lateinit var onlineCashManager: OnlineCashManager

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
        null
      }
    }

    given("결제 요청 생성 요청에서") {
      `when`("요청자와 커뮤니티가 존재할 때") {
        then("요청이 생성되고 상태가 대기 중으로 반환된다") {
          val requester = createAndPersistUser("requester@payvue.com")
          val community = createCooperativeCommunity(requester)

          val response = service.create(
            mockUserDetails("requester@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(1000L, "회비 요청", "PAYMENT", "HIGH")
          )

          response.status() shouldBe "PENDING"
          response.groupId() shouldBe community.getId().toString()
          response.requesterId() shouldBe requester.getId().toString()
        }
      }
    }

    given("결제 요청 목록 조회 요청에서") {
      `when`("커뮤니티에 요청이 존재할 때") {
        then("커뮤니티 기준 요청 목록이 반환된다") {
          val requester = createAndPersistUser("list@payvue.com")
          val community = createCooperativeCommunity(requester)
          service.create(
            mockUserDetails("list@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(500L, "첫 요청", "PAYMENT", "LOW")
          )
          service.create(
            mockUserDetails("list@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(700L, "두 번째 요청", "PAYMENT", "HIGH")
          )

          val result = service.list(
            mockUserDetails("list@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE
          )

          result shouldHaveSize 2
        }
      }
    }

    given("내 결제 요청 조회 요청에서") {
      `when`("요청자가 존재할 때") {
        then("요청자 기준 목록이 반환된다") {
          val requester = createAndPersistUser("my@payvue.com")
          val other = createAndPersistUser("other@payvue.com")
          val community = createCooperativeCommunity(requester)
          addMember(community, requester)
          addMember(community, other)
          service.create(
            mockUserDetails("my@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(300L, "내 요청", "PAYMENT", "LOW")
          )
          service.create(
            mockUserDetails("other@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(200L, "다른 요청", "PAYMENT", "LOW")
          )

          val result = service.listMy(mockUserDetails("my@payvue.com"))

          result shouldHaveSize 1
          result[0].requesterId() shouldBe requester.getId().toString()
        }
      }
    }

    given("결제 요청 승인 요청에서") {
      `when`("승인 가능한 요청이 존재할 때") {
        then("예외가 발생하고 잔액이 유지된다") {
          val admin = createAndPersistUser("admin@payvue.com")
          val payer = createAndPersistUser("payer@payvue.com")
          val community = createCooperativeCommunity(admin)
          addMember(community, payer)
          onlineCashManager.initial(community, admin, 1500L)
          val beforeBalance = onlineCashManager.possesses(community).toLong()

          val created = service.create(
            mockUserDetails("payer@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(1000L, "승인 요청", "PAYMENT", "HIGH")
          )

          assertThatThrownBy {
            service.approve(
              mockUserDetails("admin@payvue.com"),
              created.id(),
              CommunityCategory.COOPERATIVE,
              PaymentRequestApproveRequest()
            )
          }.isInstanceOf(IllegalStateException::class.java)

          val stored = paymentRequestRepository.findById(
            PaymentRequestId.of(UUID.fromString(created.id()))
          ).orElseThrow()
          val afterBalance = onlineCashManager.possesses(community).toLong()
          stored.status().name shouldBe "PENDING"
          stored.approvedDate() shouldBe null
          afterBalance shouldBe beforeBalance
        }
      }

      `when`("요청 금액이 잔액과 동일할 때") {
        then("예외가 발생하고 잔액이 유지된다") {
          val admin = createAndPersistUser("equal@payvue.com")
          val requester = createAndPersistUser("equal-request@payvue.com")
          val community = createCooperativeCommunity(admin)
          addMember(community, requester)
          onlineCashManager.initial(community, admin, 500L)
          val beforeBalance = onlineCashManager.possesses(community).toLong()

          val created = service.create(
            mockUserDetails("equal-request@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(500L, "잔액 동일", "PAYMENT", "LOW")
          )

          assertThatThrownBy {
            service.approve(
              mockUserDetails("equal@payvue.com"),
              created.id(),
              CommunityCategory.COOPERATIVE,
              PaymentRequestApproveRequest()
            )
          }.isInstanceOf(IllegalStateException::class.java)

          val afterBalance = onlineCashManager.possesses(community).toLong()
          afterBalance shouldBe beforeBalance
        }
      }

      `when`("요청 금액이 잔액보다 클 때") {
        then("예외가 발생하고 잔액이 유지된다") {
          val admin = createAndPersistUser("short@payvue.com")
          val requester = createAndPersistUser("short-request@payvue.com")
          val community = createCooperativeCommunity(admin)
          addMember(community, requester)
          onlineCashManager.initial(community, admin, 400L)
          val beforeBalance = onlineCashManager.possesses(community).toLong()

          val created = service.create(
            mockUserDetails("short-request@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(500L, "잔액 부족", "PAYMENT", "HIGH")
          )

          assertThatThrownBy {
            service.approve(
              mockUserDetails("short@payvue.com"),
              created.id(),
              CommunityCategory.COOPERATIVE,
              PaymentRequestApproveRequest()
            )
          }.isInstanceOf(IllegalStateException::class.java)

          val afterBalance = onlineCashManager.possesses(community).toLong()
          afterBalance shouldBe beforeBalance
        }
      }
    }

    given("결제 요청 거절 요청에서") {
      `when`("거절 가능한 요청이 존재할 때") {
        then("요청이 거절 상태로 변경된다") {
          val admin = createAndPersistUser("reject@payvue.com")
          val requester = createAndPersistUser("rejecter@payvue.com")
          val community = createCooperativeCommunity(admin)
          addMember(community, requester)
          val created = service.create(
            mockUserDetails("rejecter@payvue.com"),
            community.getId().getValue(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestCreateRequest(1000L, "거절 요청", "PAYMENT", "HIGH")
          )

          service.reject(
            mockUserDetails("reject@payvue.com"),
            created.id(),
            CommunityCategory.COOPERATIVE,
            PaymentRequestRejectRequest("사유")
          )

          val stored = paymentRequestRepository.findById(
            PaymentRequestId.of(UUID.fromString(created.id()))
          ).orElseThrow()
          stored.status().name shouldBe "REJECTED"
          stored.rejectionReason() shouldBe "사유"
        }
      }

      `when`("요청이 존재하지 않을 때") {
        then("예외가 발생한다") {
          assertThatThrownBy {
            service.reject(
              mockUserDetails("reject@payvue.com"),
              UUID.randomUUID().toString(),
              CommunityCategory.COOPERATIVE,
              PaymentRequestRejectRequest("사유")
            )
          }.isInstanceOf(IllegalArgumentException::class.java)
        }
      }
    }
  }

  private fun createCooperativeCommunity(user: User): Community {
    val community = communityContextManager.init(
      CommunityInitCommand(
        CommunityCategory.COOPERATIVE.name,
        UserAccountView.from(user)
      )
    )
    return community
  }

  private fun addMember(community: Community, user: User) {
    if (community is com.payvue.app.domain.community.Cooperative) {
      community.addMember(user.getId().getValue()) { true }
      communityContextManager.save(community)
    }
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
    PaymentRequestApplicationService::class,
    CommunityContextConfiguration::class,
    UserContextConfiguration::class,
    InteractionConfiguration::class,
    AssetCharacteristicConfiguration::class
  )
  @ComponentScan(
    basePackages = [
      "com.payvue.app.infrastructure",
      "com.payvue.app.domain.asset",
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
