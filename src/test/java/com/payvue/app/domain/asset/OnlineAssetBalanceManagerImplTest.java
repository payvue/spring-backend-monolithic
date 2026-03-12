package com.payvue.app.domain.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.community.Community;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("OnlineAssetBalanceManagerImpl")
class OnlineAssetBalanceManagerImplTest {

  @Test
  @DisplayName("CASH 잔액을 합산한다")
  void sumsCashBalances() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    CapabilityManager capabilityManager = mock(CapabilityManager.class);
    OnlineAssetBalanceManagerImpl manager = new OnlineAssetBalanceManagerImpl(
        repository,
        capabilityManager
    );
    TestCommunity community = new TestCommunity();

    AssetItem cash1 = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(500L, "KRW")));
    AssetItem cash2 = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    AssetItem mileage = Mileage.create(community.getId(), UserId.generate(), "mileage")
        .with(HasBalance.of(Money.of(700L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash1, cash2, mileage));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    Money total = manager.possesses(community);

    assertThat(total.getAmount().longValue()).isEqualTo(800L);
  }

  @Test
  @DisplayName("마일리지 잔액을 합산한다")
  void sumsMileageBalances() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    CapabilityManager capabilityManager = mock(CapabilityManager.class);
    OnlineAssetBalanceManagerImpl manager = new OnlineAssetBalanceManagerImpl(
        repository,
        capabilityManager
    );
    TestCommunity community = new TestCommunity();

    AssetItem cash = Cash.create(community.getId(), UserId.generate(), "cash")
        .with(HasBalance.of(Money.of(500L, "KRW")));
    AssetItem mileage1 = Mileage.create(community.getId(), UserId.generate(), "mileage")
        .with(HasBalance.of(Money.of(200L, "KRW")));
    AssetItem mileage2 = Mileage.create(community.getId(), UserId.generate(), "mileage")
        .with(HasBalance.of(Money.of(300L, "KRW")));
    Holdings holdings = Holdings.create(community, List.of(cash, mileage1, mileage2));
    when(repository.findByCommunity(community)).thenReturn(holdings);

    Money total = manager.possesses(community, AssetClassification.mileage());

    assertThat(total.getAmount().longValue()).isEqualTo(500L);
  }

  @Test
  @DisplayName("초기화 시 cash/mileage 자산이 추가된다")
  void initializesBalances() {
    HoldingsRepository repository = mock(HoldingsRepository.class);
    CapabilityManager capabilityManager = new AssetCharacteristicConfiguration().capabilityManager();
    OnlineAssetBalanceManagerImpl manager = new OnlineAssetBalanceManagerImpl(
        repository,
        capabilityManager
    );
    TestCommunity community = new TestCommunity();
    Holdings holdings = Holdings.create(community);
    when(repository.findByCommunity(community)).thenReturn(holdings);

    manager.initial(community, mockUser(), 1000L);

    ArgumentCaptor<Holdings> captor = ArgumentCaptor.forClass(Holdings.class);
    verify(repository).save(captor.capture());

    Holdings saved = captor.getValue();
    assertThat(saved.getAssets()).hasSize(2);
  }

  private User mockUser() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(UserId.generate());
    return user;
  }

  static final class TestCommunity extends Community {
    private static final RoleDescriptor ADMIN_ROLE = new TestRoleDescriptor();

    TestCommunity() {
      super("test-community", "test", UUID.randomUUID(), ADMIN_ROLE);
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
}
