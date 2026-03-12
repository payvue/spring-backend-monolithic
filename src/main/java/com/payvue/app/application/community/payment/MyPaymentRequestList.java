package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

public final class MyPaymentRequestList {
  private final UserDetails userDetails;
  private User requester;
  private Community community;
  private List<PaymentRequest> requests = List.of();
  private List<PaymentRequestResponse> responses = List.of();

  public MyPaymentRequestList(UserDetails userDetails) {
    this.userDetails = userDetails;
  }

  public UserDetails userDetails() {
    return userDetails;
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

  public List<PaymentRequest> requests() {
    return requests;
  }

  public void requests(List<PaymentRequest> requests) {
    this.requests = requests == null ? List.of() : requests;
  }

  public List<PaymentRequestResponse> responses() {
    return responses;
  }

  public void responses(List<PaymentRequestResponse> responses) {
    this.responses = responses == null ? List.of() : responses;
  }
}
