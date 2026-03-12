package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;

public final class MyPaymentRequestListFlow {
  private final MyPaymentRequestList context;

  private MyPaymentRequestListFlow(MyPaymentRequestList context) {
    this.context = context;
  }

  public static MyPaymentRequestListFlow start(UserDetails userDetails) {
    return new MyPaymentRequestListFlow(new MyPaymentRequestList(userDetails));
  }

  public MyPaymentRequestListFlow user(Consumer<MyPaymentRequestListFlow> step) {
    step.accept(this);
    return this;
  }

  public MyPaymentRequestListFlow community(Consumer<MyPaymentRequestListFlow> step) {
    step.accept(this);
    return this;
  }

  public MyPaymentRequestListFlow request(Consumer<MyPaymentRequestListFlow> step) {
    step.accept(this);
    return this;
  }

  public MyPaymentRequestListFlow map(Consumer<MyPaymentRequestListFlow> step) {
    step.accept(this);
    return this;
  }

  public MyPaymentRequestListFlow loadRequester(UserContextManager userContextManager) {
    User requester = userContextManager
        .findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.requester(requester);
    return this;
  }

  public MyPaymentRequestListFlow loadCommunity(
      CommunityContextManager communityContextManager,
      CommunityCategory category,
      UUID communityId
  ) {
    Community community = communityContextManager.get(
        new CategoryQuery(category.name(), CommunityId.of(communityId))
    );
    context.community(community);
    return this;
  }

  public MyPaymentRequestListFlow loadByRequester(PaymentRequestRepository paymentRequestRepository) {
    context.requests(paymentRequestRepository.findByRequester(context.requester()));
    return this;
  }

  public MyPaymentRequestListFlow loadByRequesterAndCommunity(
      PaymentRequestRepository paymentRequestRepository
  ) {
    Community community = context.community();
    if (community == null) {
      throw new IllegalArgumentException("커뮤니티를 찾을 수 없습니다.");
    }
    context.requests(paymentRequestRepository.findByRequesterAndCommunity(
        context.requester(),
        community
    ));
    return this;
  }

  public MyPaymentRequestListFlow toResponses(UserContextManager userContextManager) {
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
