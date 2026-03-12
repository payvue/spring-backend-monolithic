package com.payvue.core.transaction.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.characteristic.HasBalance;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.command.meta.TransactionMetaKey;
import com.payvue.shared.Money;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

@DisplayName("TransactionConfiguration")
@SpringBootTest(classes = {
    TransactionConfiguration.class,
    AssetCharacteristicConfiguration.class,
    TransactionConfigurationTest.TestConfig.class
})
class TransactionConfigurationTest {

  @TestConfiguration
  static class TestConfig {

    @Bean
    PaymentRequestRepository paymentRequestRepository() {
      return Mockito.mock(PaymentRequestRepository.class);
    }

    @Bean
    HoldingsRepository holdingsRepository() {
      return Mockito.mock(HoldingsRepository.class);
    }

    @Bean
    TransactionRepository transactionRepository() {
      return Mockito.mock(TransactionRepository.class);
    }
  }

  @Autowired
  private TransactionBindingRegistry registry;

  @Autowired
  private TransactionService transactionService;

  @Test
  @DisplayName("트랜잭션 바인딩 구성이 주요 조합을 등록한다")
  void registersExpectedBindings() {
    assertThat(registry.resolve(AssetType.CASH.name(), TransactionType.TRANSFER)).isNotNull();
    assertThat(registry.resolve(AssetType.CASH.name(), TransactionType.PAYMENT).handler())
        .isInstanceOf(PolicyAwareHandlerChain.class);
    assertThat(registry.resolve(AssetType.POINT.name(), TransactionType.PAYMENT)).isNotNull();
    assertThat(registry.resolve(AssetType.SUBSCRIPTION.name(), TransactionType.REFUND)).isNotNull();
  }

  @Test
  @DisplayName("CASH 결제는 잔액이 충분하면 COMPLETED가 되고 수수료 메타가 기록된다")
  void paymentCashCompletedWithCommission() {
    TransactionCommand command = paymentCommandWithBalances(
        Money.of(2000L, "KRW"),
        Money.of(1000L, "KRW")
    );

    Transaction result = transactionService.process(command);

    assertThat(result.interactionState()).isEqualTo(com.payvue.common.status.ExecutionState.COMPLETED);
    assertThat(result.meta().get(TransactionMetaKey.COMMISSION)).isEqualTo("500.00");
  }

  @ParameterizedTest(name = "[{index}] {0} + {1} -> {2}, commission={3}")
  @MethodSource("transactionCases")
  @DisplayName("자산/타입 조합별로 트랜잭션 컨텍스트를 검증한다")
  void transactionContextByAssetAndType(
      String assetType,
      TransactionType type,
      com.payvue.common.status.ExecutionState expectedState,
      boolean commissionExpected
  ) {
    TransactionCommand command = commandFor(assetType, type);

    if (AssetType.POINT.name().equals(assetType)
        && (type == TransactionType.PAYMENT
        || type == TransactionType.REWARD
        || type == TransactionType.REFUND)) {
      assertThatThrownBy(() -> transactionService.process(command))
          .isInstanceOf(IllegalArgumentException.class);
      return;
    }

    Transaction result = transactionService.process(command);

    assertThat(result.interactionState()).isEqualTo(expectedState);
    if (commissionExpected) {
      assertThat(result.meta().get(TransactionMetaKey.COMMISSION)).isNotNull();
    } else {
      assertThat(result.meta().get(TransactionMetaKey.COMMISSION)).isNull();
    }
  }

  @Test
  @DisplayName("CASH 결제는 잔액 부족이면 FAILED가 된다")
  void paymentCashFailsWhenBalanceInsufficient() {
    TransactionCommand command = paymentCommandWithBalances(
        Money.of(500L, "KRW"),
        Money.of(1000L, "KRW")
    );

    Transaction result = transactionService.process(command);

    assertThat(result.interactionState()).isEqualTo(com.payvue.common.status.ExecutionState.FAILED);
    assertThat(result.meta().get(TransactionMetaKey.COMMISSION)).isNull();
  }

  @Test
  @DisplayName("CASH 결제는 HasBalance가 없으면 FAILED가 된다")
  void paymentCashFailsWhenItemHasNoBalanceCapability() {
    TransactionCommand command = paymentCommandWithoutItemBalance(Money.of(2000L, "KRW"));

    Transaction result = transactionService.process(command);

    assertThat(result.interactionState()).isEqualTo(com.payvue.common.status.ExecutionState.FAILED);
    assertThat(result.meta().get(TransactionMetaKey.COMMISSION)).isNull();
  }

  @Test
  @DisplayName("CASH 이체는 정상 처리 시 COMPLETED가 된다")
  void transferCashCompleted() {
    TransactionCommand command = transferCommand(
        AssetType.CASH.name(),
        Money.of(2000L, "KRW")
    );

    Transaction result = transactionService.process(command);

    assertThat(result.interactionState()).isEqualTo(com.payvue.common.status.ExecutionState.COMPLETED);
  }

  @Test
  @DisplayName("assetType과 item.category가 불일치하면 FAILED로 기록된다")
  void assetTypeMismatchFails() {
    TransactionCommand command = mismatchedAssetTypeCommand();

    assertThatThrownBy(() -> transactionService.process(command))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Stream<Arguments> transactionCases() {
    return Stream.of(
        Arguments.of(AssetType.CASH.name(), TransactionType.PAYMENT,
            com.payvue.common.status.ExecutionState.COMPLETED, true),
        Arguments.of(AssetType.POINT.name(), TransactionType.PAYMENT,
            com.payvue.common.status.ExecutionState.FAILED, false),
        Arguments.of(AssetType.POINT.name(), TransactionType.REWARD,
            com.payvue.common.status.ExecutionState.FAILED, false),
        Arguments.of(AssetType.CASH.name(), TransactionType.REFUND,
            com.payvue.common.status.ExecutionState.FAILED, false),
        Arguments.of(AssetType.POINT.name(), TransactionType.REFUND,
            com.payvue.common.status.ExecutionState.FAILED, false),
        Arguments.of(AssetType.SUBSCRIPTION.name(), TransactionType.REFUND,
            com.payvue.common.status.ExecutionState.FAILED, false)
    );
  }

  private TransactionCommand paymentCommandWithBalances(Money cashBalance, Money itemBalance) {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());

    AssetItem cash = Cash.create(community.getId(), actorId, "cash")
        .with(HasBalance.of(cashBalance));
    Holdings holdings = Holdings.create(community, List.of(cash));
    AssetItem item = TestPayableItem.create(community.getId(), actorId, itemBalance);

    Actor actor = new Actor(community, holdings, actorId, null);
    return new TransactionCommand(
        TransactionType.PAYMENT,
        AssetType.CASH.name(),
        item,
        Money.of(1000L, "KRW"),
        actor,
        TransactionMeta.empty(),
        community,
        community
    );
  }

  private TransactionCommand commandFor(String assetType, TransactionType type) {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());

    AssetItem cash = Cash.create(community.getId(), actorId, "cash")
        .with(HasBalance.of(Money.of(2000L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash));

    AssetItem item = TestPayableItem.create(
        community.getId(),
        actorId,
        Money.of(1000L, "KRW")
    );

    Actor actor = new Actor(community, holdings, actorId, null);
    return new TransactionCommand(
        type,
        assetType,
        item,
        Money.of(1000L, "KRW"),
        actor,
        TransactionMeta.empty(),
        community,
        community
    );
  }

  private TransactionCommand paymentCommandWithoutItemBalance(Money cashBalance) {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());

    AssetItem cash = Cash.create(community.getId(), actorId, "cash")
        .with(HasBalance.of(cashBalance));
    Holdings holdings = Holdings.create(community, List.of(cash));
    AssetItem item = TestItemWithoutBalance.create(community.getId(), actorId);

    Actor actor = new Actor(community, holdings, actorId, null);
    return new TransactionCommand(
        TransactionType.PAYMENT,
        AssetType.CASH.name(),
        item,
        Money.of(1000L, "KRW"),
        actor,
        TransactionMeta.empty(),
        community,
        community
    );
  }

  private TransactionCommand transferCommand(String assetType, Money cashBalance) {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());

    AssetItem cash = Cash.create(community.getId(), actorId, "cash")
        .with(HasBalance.of(cashBalance));
    Holdings holdings = Holdings.create(community, List.of(cash));

    AssetItem item = TestPayableItem.create(
        community.getId(),
        actorId,
        Money.of(1000L, "KRW")
    );

    Actor actor = new Actor(community, holdings, actorId, null);
    return new TransactionCommand(
        TransactionType.TRANSFER,
        assetType,
        item,
        Money.of(1000L, "KRW"),
        actor,
        TransactionMeta.empty(),
        community,
        community
    );
  }

  private TransactionCommand mismatchedAssetTypeCommand() {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());

    AssetItem cash = Cash.create(community.getId(), actorId, "cash")
        .with(HasBalance.of(Money.of(2000L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash));

    AssetItem item = TestPayableItem.create(
        community.getId(),
        actorId,
        Money.of(1000L, "KRW")
    );

    Actor actor = new Actor(community, holdings, actorId, null);
    return new TransactionCommand(
        TransactionType.PAYMENT,
        AssetType.POINT.name(),
        item,
        Money.of(1000L, "KRW"),
        actor,
        TransactionMeta.empty(),
        community,
        community
    );
  }

  static final class TestCommunity extends Community {
    private static final RoleDescriptor ADMIN_ROLE = new TestRoleDescriptor();

    TestCommunity(UUID adminId) {
      super("test-community", "test", adminId, ADMIN_ROLE);
    }

    @Override
    public String getCommunityType() {
      return "TEST";
    }
  }

  static final class TestRoleDescriptor implements RoleDescriptor {
    @Override
    public String getName() {
      return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
      return true;
    }
  }

  static final class TestPayableItem extends AssetItem {

    private TestPayableItem(
        AssetItemId id,
        String name,
        CommunityId communityId,
        UserId contributorId,
        AssetClassification category,
        Set<com.payvue.characteristic.AssetCharacteristic> capabilities,
        AssetStatus status
    ) {
      super(id, name, communityId, contributorId, category, capabilities, status);
    }

    static TestPayableItem create(CommunityId communityId, UserId contributorId, Money balance) {
      return new TestPayableItem(
          AssetItemId.generate(),
          "item",
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(HasBalance.of(balance)),
          AssetStatus.ACTIVE
      );
    }
  }

  static final class TestItemWithoutBalance extends AssetItem {

    private TestItemWithoutBalance(
        AssetItemId id,
        String name,
        CommunityId communityId,
        UserId contributorId,
        AssetClassification category,
        Set<com.payvue.characteristic.AssetCharacteristic> capabilities,
        AssetStatus status
    ) {
      super(id, name, communityId, contributorId, category, capabilities, status);
    }

    static TestItemWithoutBalance create(CommunityId communityId, UserId contributorId) {
      return new TestItemWithoutBalance(
          AssetItemId.generate(),
          "item",
          communityId,
          contributorId,
          AssetClassification.cash(),
          Set.of(),
          AssetStatus.ACTIVE
      );
    }
  }
}
