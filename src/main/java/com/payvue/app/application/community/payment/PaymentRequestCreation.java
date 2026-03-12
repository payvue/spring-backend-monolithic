package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestCreateRequest;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentRequestCreation {
  private final UserDetails userDetails;
  private final UUID communityId;
  private final PaymentRequestCreateRequest request;
  private User requester;
  private Community community;
  private PaymentRequest paymentRequest;
  private Instant createdAt;

  public PaymentRequestCreation(
      UserDetails userDetails,
      UUID communityId,
      PaymentRequestCreateRequest request
  ) {
    this.userDetails = userDetails;
    this.communityId = communityId;
    this.request = request;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public UUID communityId() {
    return communityId;
  }

  public PaymentRequestCreateRequest request() {
    return request;
  }

  public User requester() {
    return requester;
  }

  public void requester(User requester) {
    this.requester = requester;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public PaymentRequest paymentRequest() {
    return paymentRequest;
  }

  public void paymentRequest(PaymentRequest paymentRequest) {
    this.paymentRequest = paymentRequest;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public void createdAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
