package com.payvue.app.application.asset.create;

import com.payvue.app.domain.asset.category.GameItem;
import com.payvue.app.domain.asset.category.GiftCard;
import com.payvue.app.domain.asset.category.MusicItem;
import com.payvue.app.domain.asset.category.ShoppingItem;
import com.payvue.app.presentation.rest.hold.AssetOnlineCreateRequest;
import com.payvue.characteristic.Consumable;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class OnlineAssetCreateFlow {
  private final OnlineAssetCreateContext context;

  private OnlineAssetCreateFlow(OnlineAssetCreateContext context) {
    this.context = context;
  }

  public static OnlineAssetCreateFlow start(
      UserDetails userDetails,
      AssetOnlineCreateRequest request
  ) {
    return new OnlineAssetCreateFlow(new OnlineAssetCreateContext(userDetails, request));
  }

  public OnlineAssetCreateFlow user(Consumer<OnlineAssetCreateFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetCreateFlow community(Consumer<OnlineAssetCreateFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetCreateFlow holdings(Consumer<OnlineAssetCreateFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetCreateFlow asset(Consumer<OnlineAssetCreateFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetCreateFlow save(Consumer<OnlineAssetCreateFlow> step) {
    step.accept(this);
    return this;
  }

  public OnlineAssetCreateFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public OnlineAssetCreateFlow loadPersonalCommunity(CommunityContextManager communityContextManager) {
    Community community = communityContextManager.resolve(context.user(), "PERSONAL")
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    context.community(community);
    return this;
  }

  public OnlineAssetCreateFlow loadHoldings(HoldingsRepository holdingsRepository) {
    Holdings holdings = holdingsRepository.findByCommunity(context.community());
    context.holdings(holdings);
    return this;
  }

  public OnlineAssetCreateFlow addOnlineAsset() {
    AssetOnlineCreateRequest request = context.request();
    validateRequest(request);
    AssetItem item = createItem(request, context.community(), context.user());
    context.holdings().getAssets().add(item);
    context.asset(item);
    return this;
  }

  public OnlineAssetCreateFlow persist(HoldingsRepository holdingsRepository) {
    holdingsRepository.save(context.holdings());
    return this;
  }

  public OnlineAssetCreateContext end() {
    return context;
  }

  private void validateRequest(AssetOnlineCreateRequest request) {
    Objects.requireNonNull(request, "request는 필수입니다.");
    if (request.name() == null || request.name().isBlank()) {
      throw new IllegalArgumentException("name은 비어 있을 수 없습니다.");
    }
    if (request.category() == null || request.category().isBlank()) {
      throw new IllegalArgumentException("category는 비어 있을 수 없습니다.");
    }
    if (request.amount() == null || request.amount() <= 0L) {
      throw new IllegalArgumentException("amount는 1 이상이어야 합니다.");
    }
  }

  private AssetItem createItem(
      AssetOnlineCreateRequest request,
      Community community,
      User user
  ) {
    String category = request.category().trim().toUpperCase(Locale.ROOT);
    AssetItem item = switch (category) {
      case "GAME" -> GameItem.create(community.getId(), user.getId(), request.name());
      case "MUSIC" -> MusicItem.create(community.getId(), user.getId(), request.name());
      case "GIFT" -> GiftCard.create(community.getId(), user.getId(), request.name());
      case "SHOPPING" -> ShoppingItem.create(community.getId(), user.getId(), request.name());
      default -> throw new IllegalArgumentException("지원하지 않는 category입니다: " + request.category());
    };
    int quantity = toQuantity(request.amount());
    item.with(Consumable.multipleUse(quantity));
    applyStatus(item, request.status());
    return item;
  }

  private int toQuantity(Long amount) {
    if (amount == null) {
      return 0;
    }
    if (amount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("amount가 너무 큽니다.");
    }
    return amount.intValue();
  }

  private void applyStatus(AssetItem item, String status) {
    AssetStatus assetStatus = parseStatus(status);
    switch (assetStatus) {
      case ACTIVE -> {
      }
      case SUSPENDED -> item.suspend();
      case EXPIRED -> item.expire();
      case USED -> item.use();
      default -> throw new IllegalArgumentException("지원하지 않는 status입니다: " + status);
    }
  }

  private AssetStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return AssetStatus.ACTIVE;
    }
    String normalized = status.trim().toUpperCase(Locale.ROOT);
    if ("INACTIVE".equals(normalized)) {
      return AssetStatus.SUSPENDED;
    }
    return AssetStatus.valueOf(normalized);
  }
}
