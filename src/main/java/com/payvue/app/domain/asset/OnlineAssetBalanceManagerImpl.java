package com.payvue.app.domain.asset;

import com.payvue.core.asset.AssetClassification;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.app.domain.asset.category.Mileage;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.app.domain.asset.operation.AcquireBalanceOperation;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnlineAssetBalanceManagerImpl implements OnlineCashManager {

  private final HoldingsRepository holdingsRepository;
  private final CapabilityManager capabilityManager;

  public OnlineAssetBalanceManagerImpl(
      HoldingsRepository holdingsRepository,
      CapabilityManager capabilityManager
  ) {
    this.holdingsRepository = java.util.Objects.requireNonNull(
        holdingsRepository, "holdingsRepository는 필수입니다"
    );
    this.capabilityManager = java.util.Objects.requireNonNull(
        capabilityManager, "capabilityManager는 필수입니다"
    );
  }

  @Override
  public Money possesses(Community community) {
    return resolveBalance(community, BalanceCategory.CASH);
  }

  @Override
  public Money possesses(Community community, AssetClassification category) {
    java.util.Objects.requireNonNull(category, "category는 필수입니다");
    return resolveBalance(community, BalanceCategory.from(category));
  }

  private Money resolveBalance(Community community, BalanceCategory category) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(category, "category는 필수입니다");
    Holdings holdings = holdingsRepository.findByCommunity(community);
    List<AssetItem> items = holdings.getAssets();
    Money total = Money.zero("KRW");
    for (AssetItem item : items) {
      if (category.matches(item)) {
        Optional<HasBalance> balance = item.getCapability(HasBalance.class);
        if (balance.isPresent()) {
          total = total.add(balance.get().balance());
        }
      }
    }
    return total;
  }

  @Override
  @Transactional
  public void credit(Community community, User owner, long amount) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(owner, "owner는 필수입니다");
    if (amount <= 0) {
      throw new IllegalArgumentException("amount는 0보다 커야 합니다");
    }
    Holdings holdings = holdingsRepository.findByCommunity(community);
    AssetItem cash = Cash.create(
        community.getId(),
        owner.getId(),
        AssetClassification.cash().getValue()
    ).with(HasBalance.of(Money.of(amount, "KRW")));
    holdings.dealWith(cash, new AcquireBalanceOperation(holdings, capabilityManager));
    holdingsRepository.save(holdings);
  }

  @Override
  @Transactional
  public void initial(Community community, User owner, long initialAmount) {
    java.util.Objects.requireNonNull(community, "community는 필수입니다");
    java.util.Objects.requireNonNull(owner, "owner는 필수입니다");
    if (initialAmount < 0) {
      throw new IllegalArgumentException("initialAmount는 음수일 수 없습니다");
    }
    Holdings holdings = holdingsRepository.findByCommunity(community);
    AssetItem cash = Cash.create(
        community.getId(),
        owner.getId(),
        AssetClassification.cash().getValue()
    )
        .with(HasBalance.of(Money.of(initialAmount, "KRW")));
    AssetItem mileage = Mileage.create(
        community.getId(),
        owner.getId(),
        AssetClassification.mileage().getValue()
    ).with(HasBalance.of(Money.of(initialAmount, "KRW")));

    holdings.dealWith(cash, new AcquireBalanceOperation(holdings, capabilityManager));
    holdings.dealWith(mileage, new AcquireBalanceOperation(holdings, capabilityManager));
    holdingsRepository.save(holdings);
  }

  private enum BalanceCategory {
    CASH {
      @Override
      boolean matches(AssetItem item) {
        return item.getCategory().isCash();
      }
    },
    MILEAGE {
      @Override
      boolean matches(AssetItem item) {
        return item.getCategory().isMileage();
      }
    };

    abstract boolean matches(AssetItem item);

    static BalanceCategory from(AssetClassification category) {
      if (category.isCash()) {
        return CASH;
      }
      if (category.isMileage()) {
        return MILEAGE;
      }
      throw new IllegalArgumentException("지원하지 않는 자산 분류입니다: " + category);
    }
  }
}
