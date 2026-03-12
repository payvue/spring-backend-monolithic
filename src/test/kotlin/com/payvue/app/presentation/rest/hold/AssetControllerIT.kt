package com.payvue.app.presentation.rest.hold

import com.payvue.app.application.asset.AssetApplicationService
import com.payvue.app.application.summary.SummaryApplicationService
import com.payvue.app.domain.summary.BalancesByCategory
import com.payvue.app.domain.auth.AuthContextManager
import com.payvue.app.domain.security.SessionTokenRepository
import com.payvue.app.domain.security.TokenProvider
import com.payvue.supporting.user.command.User
import com.payvue.supporting.user.command.UserPasswordGuard
import com.payvue.supporting.user.command.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.nullValue
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import com.payvue.supporting.user.command.UserAccountView

@SpringBootTest(classes = [com.payvue.PayvueApplication::class, AssetControllerIT.TestSecurityBeans::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetControllerIT : DescribeSpec() {

  override fun extensions() = listOf(SpringExtension)

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var sessionTokenRepository: SessionTokenRepository

  @MockBean
  private lateinit var assetApplicationService: AssetApplicationService

  @MockBean
  private lateinit var summaryApplicationService: SummaryApplicationService

  @MockBean
  private lateinit var authContextManager: AuthContextManager

  init {
    describe("자산 컨트롤러 통합 테스트") {
      context("온라인 자산 요약 조회 요청에서") {
        it("인증된 요청이면 목록이 반환된다") {
          val user = createAndPersistUser("asset-it@payvue.com")
          val token = UserAccountView.from(user).email()
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          `when`(assetApplicationService.online(any()))
            .thenReturn(
              listOf(
                AssetOnlineItemResponse(
                  "asset-1",
                  "payvue 캐시",
                  "CASH",
                  1200L,
                  1200L,
                  null,
                  null,
                  "ACTIVE",
                  null
                )
              )
            )

          mockMvc.get("/asset/summary/online") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data[0].id") { value("asset-1") }
            jsonPath("$.data[0].name") { value("payvue 캐시") }
            jsonPath("$.data[0].category") { value("CASH") }
            jsonPath("$.data[0].amount") { value(1200) }
            jsonPath("$.data[0].balance") { value(1200) }
            jsonPath("$.data[0].remainingAmount") { value(nullValue()) }
            jsonPath("$.data[0].purchaseDate") { value(nullValue()) }
            jsonPath("$.data[0].status") { value("ACTIVE") }
            jsonPath("$.data[0].description") { value(nullValue()) }
          }
        }
      }

      context("자산 요약 조회 요청에서") {
        it("인증된 요청이면 요약 정보가 반환된다") {
          val user = createAndPersistUser("summary-it@payvue.com")
          val token = UserAccountView.from(user).email()
          sessionTokenRepository.save(token, token, Duration.ofMinutes(5))
          val summary = BalancesByCategory.ofAggregate(
            1000L,
            1000L,
            1500L,
            700L,
            700L,
            900L,
            300L,
            300L,
            600L,
            mapOf("GIFT" to 500L, "MUSIC" to 1000L),
            "KRW"
          )
          `when`(summaryApplicationService.accumulate(any(), any()))
            .thenReturn(summary)

          mockMvc.get("/asset/summary") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
          }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("SUCCESS") }
            jsonPath("$.data.total") { value(1000) }
            jsonPath("$.data.totalValue") { value(1000) }
            jsonPath("$.data.totalAssetValue") { value(1500) }
            jsonPath("$.data.personal") { value(700) }
            jsonPath("$.data.personalValue") { value(700) }
            jsonPath("$.data.personalAssetValue") { value(900) }
            jsonPath("$.data.shared") { value(300) }
            jsonPath("$.data.sharedValue") { value(300) }
            jsonPath("$.data.sharedAssetValue") { value(600) }
            jsonPath("$.data.monthlyChangeRate") { value(0.0) }
            jsonPath("$.data.changeRate") { value(0.0) }
            jsonPath("$.data.monthlyChange") { value(0) }
            jsonPath("$.data.currency") { value("KRW") }
            jsonPath("$.data.categories[0].category") { value("GIFT") }
            jsonPath("$.data.categories[0].amount") { value(500) }
            jsonPath("$.data.categories[0].ratio") { value(33) }
            jsonPath("$.data.categories[1].category") { value("MUSIC") }
            jsonPath("$.data.categories[1].amount") { value(1000) }
            jsonPath("$.data.categories[1].ratio") { value(66) }
          }
        }
      }
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

  @TestConfiguration
  open class TestSecurityBeans {
    @Bean
    @Primary
    open fun tokenProvider(): TokenProvider {
      return object : TokenProvider {
        override fun generate(
          authenticatedPrincipal: com.payvue.supporting.user.command.AuthenticatedPrincipal,
          type: String
        ): String {
          return authenticatedPrincipal.email()
        }

        override fun validate(token: String): Boolean {
          return token.isNotBlank()
        }

        override fun getSubject(token: String): String {
          return token
        }

        override fun getClaims(token: String): Map<String, Any> {
          return mapOf("sub" to token)
        }
      }
    }

    @Bean
    @Primary
    open fun sessionTokenRepository(): SessionTokenRepository {
      return InMemorySessionTokenRepository()
    }
  }

  class InMemorySessionTokenRepository : SessionTokenRepository {
    private val tokens = ConcurrentHashMap<String, String>()

    override fun save(token: String, subject: String, ttl: Duration) {
      tokens[token] = subject
    }

    override fun isActive(token: String): Boolean {
      return tokens.containsKey(token)
    }

    override fun remove(token: String) {
      tokens.remove(token)
    }

    override fun delete(user: User) {
      tokens.entries.removeIf { it.value == UserAccountView.from(user).email() }
    }

    override fun delete(view: com.payvue.supporting.user.command.UserAccountView) {
      tokens.entries.removeIf { it.value == view.email() }
    }
  }
}
