package com.payvue.app.application.pay.confirm;

import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;

public final class PayCompletion {
  private final String payId;
  private final String resultCode;
  private final String reserveId;
  private final String paymentId;

  private Payment payment;
  private OrderItem order;
  private Community community;
  private User owner;
  private Holdings holdings;
  private Transaction transaction;

  PayCompletion(String payId, String resultCode, String reserveId, String paymentId) {
    this.payId = payId;
    this.resultCode = resultCode;
    this.reserveId = reserveId;
    this.paymentId = paymentId;
  }

  public String payId() {
    return payId;
  }

  public String resultCode() {
    return resultCode;
  }

  public String reserveId() {
    return reserveId;
  }

  public String paymentId() {
    return paymentId;
  }

  public Payment payment() {
    return payment;
  }

  public void payment(Payment payment) {
    this.payment = payment;
  }

  public OrderItem order() {
    return order;
  }

  public void order(OrderItem order) {
    this.order = order;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public User owner() {
    return owner;
  }

  public void owner(User owner) {
    this.owner = owner;
  }

  public Holdings holdings() {
    return holdings;
  }

  public void holdings(Holdings holdings) {
    this.holdings = holdings;
  }

  public Transaction transaction() {
    return transaction;
  }

  public void transaction(Transaction transaction) {
    this.transaction = transaction;
  }
}
