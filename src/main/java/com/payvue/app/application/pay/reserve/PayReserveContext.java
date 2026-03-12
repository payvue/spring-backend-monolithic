package com.payvue.app.application.pay.reserve;

import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.core.community.Community;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

public final class PayReserveContext {
  private final UserDetails userDetails;
  private final PaymentProvider provider;
  private final PaymentModeType modeType;
  private final OrderItem order;
  private final String bankCode;

  private Payment payment;
  private User user;
  private Community community;

  PayReserveContext(
      UserDetails userDetails,
      PaymentProvider provider,
      PaymentModeType modeType,
      OrderItem order,
      String bankCode
  ) {
    this.userDetails = userDetails;
    this.provider = provider;
    this.modeType = modeType;
    this.order = order;
    this.bankCode = bankCode;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public PaymentProvider provider() {
    return provider;
  }

  public PaymentModeType modeType() {
    return modeType;
  }

  public OrderItem order() {
    return order;
  }

  public String bankCode() {
    return bankCode;
  }

  public Payment payment() {
    return payment;
  }

  public void payment(Payment payment) {
    this.payment = payment;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }
}
