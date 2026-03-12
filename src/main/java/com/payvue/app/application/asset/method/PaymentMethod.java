package com.payvue.app.application.asset.method;

import com.payvue.app.presentation.rest.hold.PaymentMethodResponse;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentMethod {
  private final UserDetails userDetails;
  private List<PaymentMethodResponse> responses = List.of();

  public PaymentMethod(UserDetails userDetails) {
    this.userDetails = userDetails;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public List<PaymentMethodResponse> responses() {
    return responses;
  }

  public void responses(List<PaymentMethodResponse> responses) {
    this.responses = responses == null ? List.of() : responses;
  }
}
