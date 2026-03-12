package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.core.community.Community;
import java.util.List;
import java.util.UUID;

public final class PaymentRequestListing {
  private final UUID communityId;
  private Community community;
  private List<PaymentRequest> requests = List.of();
  private List<PaymentRequestResponse> responses = List.of();

  public PaymentRequestListing(UUID communityId) {
    this.communityId = communityId;
  }

  public UUID communityId() {
    return communityId;
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
