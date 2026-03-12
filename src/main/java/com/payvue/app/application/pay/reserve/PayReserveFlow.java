package com.payvue.app.application.pay.reserve;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.app.domain.payment.PaymentService;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentRepository;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.PasswordPrincipal;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class PayReserveFlow {
  private final PayReserveContext context;

  private PayReserveFlow(PayReserveContext context) {
    this.context = context;
  }

  public static PayReserveFlow start(
      UserDetails userDetails,
      PaymentProvider provider,
      PaymentModeType modeType,
      OrderItem order,
      String bankCode
  ) {
    return new PayReserveFlow(new PayReserveContext(
        Objects.requireNonNull(userDetails, "userDetails는 필수입니다"),
        Objects.requireNonNull(provider, "provider는 필수입니다"),
        Objects.requireNonNull(modeType, "modeType은 필수입니다"),
        Objects.requireNonNull(order, "order는 필수입니다"),
        bankCode
    ));
  }

  public PayReserveFlow user(Consumer<PayReserveFlow> step) {
    step.accept(this);
    return this;
  }

  public PayReserveFlow community(Consumer<PayReserveFlow> step) {
    step.accept(this);
    return this;
  }

  public PayReserveFlow payment(Consumer<PayReserveFlow> step) {
    step.accept(this);
    return this;
  }

  public PayReserveFlow save(Consumer<PayReserveFlow> step) {
    step.accept(this);
    return this;
  }

  public PayReserveFlow reserve(PaymentService paymentService) {
    if (context.modeType() == PaymentModeType.VIRTUAL_ACCOUNT) {
      context.payment(paymentService.virtualAccountReserve(
          context.provider(),
          context.order(),
          context.bankCode()
      ));
      return this;
    }
    context.payment(paymentService.reserve(context.provider(), context.modeType(), context.order()));
    return this;
  }

  public PayReserveFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public PayReserveFlow loadPersonalCommunity(CommunityContextManager communityContextManager) {
    Community community = communityContextManager
        .resolve(context.user(), CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));
    context.community(community);
    return this;
  }

  public PayReserveFlow enrichMetadata() {
    Map<String, String> metadata = new HashMap<>(context.payment().metadata());

    metadata.put("communityId", context.community().getId().toString());
    metadata.put("ownerEmail", PasswordPrincipal.from(context.user()).email());

    OrderItem order = context.order();
    metadata.put("amount", String.valueOf(order.totalPrice().toLong()));
    metadata.put("orderId", order.orderId().toString());
    metadata.put("orderName", order.name());
    metadata.put("orderQuantity", String.valueOf(order.quantity()));
    metadata.put("orderUnitPrice", String.valueOf(order.unitPrice().toLong()));
    metadata.put("orderTotalPrice", String.valueOf(order.totalPrice().toLong()));
    metadata.put("assetType", "CASH");

    Payment enriched = Payment.of(
        context.payment().getId(),
        context.payment().status(),
        context.payment().paymentModeType(),
        context.payment().providerId(),
        context.payment().externalTransactionId(),
        metadata
    );
    context.payment(enriched);
    return this;
  }

  public PayReserveFlow persist(PaymentRepository paymentRepository) {
    paymentRepository.save(context.payment());
    return this;
  }

  public PayReserveContext end() {
    return context;
  }
}
