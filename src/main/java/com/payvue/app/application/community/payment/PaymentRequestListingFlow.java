package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PaymentRequestListingFlow {
  private final PaymentRequestListing context;

  private PaymentRequestListingFlow(PaymentRequestListing context) {
    this.context = context;
  }

  public static PaymentRequestListingFlow start(UUID communityId) {
    return new PaymentRequestListingFlow(new PaymentRequestListing(communityId));
  }

  public PaymentRequestListingFlow community(Consumer<PaymentRequestListingFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestListingFlow request(Consumer<PaymentRequestListingFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestListingFlow map(Consumer<PaymentRequestListingFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestListingFlow loadCommunity(
      CommunityContextManager communityContextManager,
      CommunityCategory category
  ) {
    Community community = communityContextManager.get(
        new CategoryQuery(
            category.name(),
            CommunityId.of(context.communityId())
        )
    );
    context.community(community);
    return this;
  }

  public PaymentRequestListingFlow loadByCommunity(PaymentRequestRepository paymentRequestRepository) {
    context.requests(paymentRequestRepository.findByCommunity(context.community()));
    return this;
  }

  public PaymentRequestListingFlow toResponses(UserContextManager userContextManager) {
    Map<String, String> requesterNames = context.requests().stream()
        .map(request -> request.requesterId())
        .distinct()
        .collect(Collectors.toMap(
            requesterId -> requesterId,
            requesterId -> userContextManager
                .findById(UserId.of(UUID.fromString(requesterId)), UserAccountView.class)
                .map(UserAccountView::displayName)
                .orElse("unknown")
        ));
    context.responses(context.requests().stream()
        .map(request -> PaymentRequestResponse.from(
            request,
            requesterNames.getOrDefault(request.requesterId(), "unknown")
        ))
        .toList());
    return this;
  }

  public List<PaymentRequestResponse> end() {
    return context.responses();
  }
}
