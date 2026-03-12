package com.payvue.app.application.account.query;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.shared.Money;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountInfoFlow {
  private final AccountInfo context;

  private AccountInfoFlow(AccountInfo context) {
    this.context = context;
  }

  public static AccountInfoFlow start(UserDetails userDetails) {
    return new AccountInfoFlow(new AccountInfo(
        Objects.requireNonNull(userDetails, "userDetails must not be null")
    ));
  }

  public AccountInfoFlow user(Consumer<AccountInfoFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountInfoFlow community(Consumer<AccountInfoFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountInfoFlow asset(Consumer<AccountInfoFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountInfoFlow map(Consumer<AccountInfoFlow> step) {
    step.accept(this);
    return this;
  }

  public AccountInfoFlow loadUser(UserContextManager userContextManager) {
    Objects.requireNonNull(userContextManager, "userContextManager must not be null");
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    context.view(UserAccountView.from(user));
    return this;
  }

  public AccountInfoFlow loadPersonalCommunity(CommunityContextManager communityContextManager) {
    Objects.requireNonNull(communityContextManager, "communityContextManager must not be null");
    Optional<Community> personal = communityContextManager.resolve(context.user(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst();
    context.personalCommunity(personal);
    return this;
  }

  public AccountInfoFlow loadAssets(OnlineCashManager cashManager) {
    Objects.requireNonNull(cashManager, "cashManager must not be null");
    Optional<Community> personal = context.personalCommunity();
    if (personal.isEmpty()) {
      context.cash(Money.won(0L));
      context.mileage(Money.won(0L));
      return this;
    }
    Community community = personal.get();
    context.cash(cashManager.possesses(community, AssetClassification.cash()));
    context.mileage(cashManager.possesses(community, AssetClassification.mileage()));
    return this;
  }

  public AccountInfoFlow toResult() {
    UserAccountView view = Objects.requireNonNull(context.view(), "view must not be null");
    AccountInfoResult.Account account = new AccountInfoResult.Account(
        view.loginId(),
        context.user().getId().getValue().toString(),
        new AccountInfoResult.Status(null, null)
    );
    AccountInfoResult.Info info = new AccountInfoResult.Info(
        view.displayName(),
        new AccountInfoResult.Contact(view.email(), view.phoneNumber()),
        null
    );
    Money cash = Objects.requireNonNull(context.cash(), "cash must not be null");
    Money mileage = Objects.requireNonNull(context.mileage(), "mileage must not be null");
    AccountInfoResult.Asset asset = new AccountInfoResult.Asset(
        cash.toLong(),
        mileage.toLong()
    );
    context.result(new AccountInfoResult(account, info, asset));
    return this;
  }

  public AccountInfoResult end() {
    return context.result();
  }
}
