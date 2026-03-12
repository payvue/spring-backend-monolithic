package com.payvue.core.transaction.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.payvue.characteristic.HasBalance;
import com.payvue.common.status.ExecutionState;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.shared.Money;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.user.command.UserId;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("TransferTransactionHandler")
class TransferTransactionHandlerTest {

  @Test
  @DisplayName("승인 대기인 경우 자산을 정지하고 PENDING_APPROVAL 상태를 반환한다")
  void returnsPendingWhenApprovalIsRequired() {
    InteractionExecutor executor = Mockito.mock(InteractionExecutor.class);
    HoldingsRepository holdingsRepository = Mockito.mock(HoldingsRepository.class);
    Interaction interaction = interaction(ExecutionState.PENDING_APPROVAL, true, false);
    Mockito.when(executor.executeInternal(any(), any()))
        .thenReturn(interaction);

    TransactionCommand command = command(TransactionType.TRANSFER, true);
    Mockito.when(holdingsRepository.findByCommunity(any()))
        .thenReturn(Holdings.create(command.to()));
    TransferTransactionHandler handler = new TransferTransactionHandler(executor, holdingsRepository);

    Transaction result = handler.handle(command);

    assertThat(result.interactionState()).isEqualTo(ExecutionState.PENDING_APPROVAL);
    assertThat(command.item().getStatus()).isEqualTo(AssetStatus.SUSPENDED);
  }

  @Test
  @DisplayName("완료된 상호작용이면 COMPLETED 상태를 반환한다")
  void returnsCompletedWhenInteractionCompleted() {
    InteractionExecutor executor = Mockito.mock(InteractionExecutor.class);
    HoldingsRepository holdingsRepository = Mockito.mock(HoldingsRepository.class);
    Interaction interaction = interaction(ExecutionState.COMPLETED, false, true);
    Mockito.when(executor.executeInternal(any(), any()))
        .thenReturn(interaction);

    TransactionCommand command = command(TransactionType.TRANSFER, true);
    Mockito.when(holdingsRepository.findByCommunity(any()))
        .thenReturn(Holdings.create(command.to()));
    TransferTransactionHandler handler = new TransferTransactionHandler(executor, holdingsRepository);

    Transaction result = handler.handle(command);

    assertThat(result.interactionState()).isEqualTo(ExecutionState.COMPLETED);
  }

  @Test
  @DisplayName("그 외 상호작용은 FAILED 상태를 반환한다")
  void returnsFailedWhenInteractionFailed() {
    InteractionExecutor executor = Mockito.mock(InteractionExecutor.class);
    HoldingsRepository holdingsRepository = Mockito.mock(HoldingsRepository.class);
    Interaction interaction = interaction(ExecutionState.FAILED, false, false);
    Mockito.when(executor.executeInternal(any(), any()))
        .thenReturn(interaction);

    TransactionCommand command = command(TransactionType.TRANSFER, true);
    Mockito.when(holdingsRepository.findByCommunity(any()))
        .thenReturn(Holdings.create(command.to()));
    TransferTransactionHandler handler = new TransferTransactionHandler(executor, holdingsRepository);

    Transaction result = handler.handle(command);

    assertThat(result.interactionState()).isEqualTo(ExecutionState.FAILED);
  }

  private Interaction interaction(ExecutionState state, boolean pending, boolean completed) {
    Interaction interaction = Mockito.mock(Interaction.class);
    Mockito.when(interaction.isPendingApproval()).thenReturn(pending);
    Mockito.when(interaction.isCompleted()).thenReturn(completed);
    Mockito.when(interaction.getStatus()).thenReturn(state);
    return interaction;
  }

  private TransactionCommand command(TransactionType type, boolean withBalance) {
    UserId actorId = UserId.generate();
    TestCommunity community = new TestCommunity(actorId.getValue());
    Holdings holdings = Holdings.create(community);
    AssetItem item = TestAssetItem.create(community.getId(), actorId, withBalance);
    Actor actor = new Actor(community, holdings, actorId, null);

    return new TransactionCommand(
        type,
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

  static final class TestAssetItem extends AssetItem {

    private TestAssetItem(
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

    static TestAssetItem create(CommunityId communityId, UserId contributorId, boolean withBalance) {
      Set<com.payvue.characteristic.AssetCharacteristic> capabilities = withBalance
          ? Set.of(HasBalance.of(Money.of(1000L, "KRW")))
          : Collections.emptySet();
      return new TestAssetItem(
          AssetItemId.generate(),
          "asset-item",
          communityId,
          contributorId,
          AssetClassification.cash(),
          capabilities,
          AssetStatus.ACTIVE
      );
    }
  }
}
