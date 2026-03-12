package com.payvue.app.application.asset.method;

import com.payvue.app.presentation.rest.hold.PaymentMethodResponse;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentMethodFlow {
  private final PaymentMethod context;

  private PaymentMethodFlow(PaymentMethod context) {
    this.context = context;
  }

  public static PaymentMethodFlow start(UserDetails userDetails) {
    return new PaymentMethodFlow(new PaymentMethod(userDetails));
  }

  public PaymentMethodFlow user(Consumer<PaymentMethodFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentMethodFlow result(Consumer<PaymentMethodFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentMethodFlow loadUser(UserContextManager userContextManager) {
    userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    return this;
  }

  public PaymentMethodFlow buildResponses() {
    context.responses(List.of(
        new PaymentMethodResponse("pm-001", "card", "결제 카드", "신한카드", true, "1111 **** **** ****"),
        new PaymentMethodResponse("pm-003", "wallet", "신규 가입 유저 혜택 지원", "Payvue 서비스", false, null)
    ));
    return this;
  }

  public List<PaymentMethodResponse> end() {
    return context.responses();
  }
}
