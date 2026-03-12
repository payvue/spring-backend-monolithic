package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestApproveRequest;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentRequestApprove {
  private final UserDetails userDetails;
  private final String requestId;
  private final PaymentRequestApproveRequest request;
  private PaymentRequest paymentRequest;
  private Community community;
  private User requester;
  private User approver;

  public PaymentRequestApprove(
      UserDetails userDetails,
      String requestId,
      PaymentRequestApproveRequest request
  ) {
    this.userDetails = userDetails;
    this.requestId = requestId;
    this.request = request;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public String requestId() {
    return requestId;
  }

  public PaymentRequestApproveRequest request() {
    return request;
  }

  public PaymentRequest paymentRequest() {
    return paymentRequest;
  }

  public void paymentRequest(PaymentRequest paymentRequest) {
    this.paymentRequest = paymentRequest;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public User requester() {
    return requester;
  }

  public void requester(User requester) {
    this.requester = requester;
  }

  public User approver() {
    return approver;
  }

  public void approver(User approver) {
    this.approver = approver;
  }
}
