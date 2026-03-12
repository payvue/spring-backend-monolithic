package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.presentation.rest.community.PaymentRequestRejectRequest;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.Participant;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.PaymentRequestIntent;
import com.payvue.supporting.interaction.RejectDecision;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.Trigger;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentRequestRejectFlow {
  private final PaymentRequestReject context;

  private PaymentRequestRejectFlow(PaymentRequestReject context) {
    this.context = context;
  }

  public static PaymentRequestRejectFlow start(
      UserDetails userDetails,
      String requestId,
      PaymentRequestRejectRequest request
  ) {
    return new PaymentRequestRejectFlow(new PaymentRequestReject(userDetails, requestId, request));
  }

  public PaymentRequestRejectFlow request(Consumer<PaymentRequestRejectFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestRejectFlow community(Consumer<PaymentRequestRejectFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestRejectFlow user(Consumer<PaymentRequestRejectFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestRejectFlow interaction(Consumer<PaymentRequestRejectFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestRejectFlow notify(Consumer<PaymentRequestRejectFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestRejectFlow loadRequest(PaymentRequestRepository paymentRequestRepository) {
    PaymentRequest paymentRequest = paymentRequestRepository.findById(
        PaymentRequestId.of(UUID.fromString(context.requestId()))
    ).orElseThrow(() -> new IllegalArgumentException("결제 요청을 찾을 수 없습니다."));
    context.paymentRequest(paymentRequest);
    return this;
  }

  public PaymentRequestRejectFlow loadCommunity(
      CommunityContextManager communityContextManager,
      CommunityCategory category
  ) {
    Community community = communityContextManager.get(
        new CategoryQuery(
            category.name(),
            CommunityId.of(UUID.fromString(context.paymentRequest().groupId()))
        )
    );
    context.community(community);
    return this;
  }

  public PaymentRequestRejectFlow loadRequester(UserContextManager userContextManager) {
    PaymentRequest paymentRequest = context.paymentRequest();
    if (paymentRequest == null) {
      throw new IllegalArgumentException("결제 요청을 찾을 수 없습니다.");
    }
    User requester = userContextManager.findById(
        UserId.of(UUID.fromString(paymentRequest.requesterId())),
        User.class
    ).orElseThrow(() -> new IllegalArgumentException("요청자를 찾을 수 없습니다."));
    context.requester(requester);
    return this;
  }

  public PaymentRequestRejectFlow loadApprover(UserContextManager userContextManager) {
    User approver = userContextManager
        .findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.approver(approver);
    return this;
  }

  public PaymentRequestRejectFlow reject(InteractionExecutor interactionExecutor) {
    Participant initiator = Community.to(context.community(), context.approver().getId().getValue());
    Interaction interaction = interactionExecutor.executeInternal(
        new RejectDecision<>(
            new PaymentRequestIntent(
                initiator,
                context.paymentRequest().id().toString(),
                context.paymentRequest().groupId()
            ),
            context.request().rejectionReason()
        ),
        context.community()
    );
    if (interaction.isFailed()) {
      throw new IllegalStateException("요청 거부에 실패했습니다.");
    }
    return this;
  }

  public PaymentRequestRejectFlow sendAlarm(AlarmContextManager alarmContextManager) {
    Community community = context.community();
    User approver = context.approver();
    User requester = context.requester();
    if (community == null || approver == null || requester == null) {
      return this;
    }
    UserAccountView approverView = UserAccountView.from(approver);
    Trigger trigger = new Trigger(
        approverView.displayName(),
        community.getParticipantRole(approver.getId().getValue()).getName()
    );
    InAppAlarm alarm = AlarmFactory.createPaymentRequestRejectedAlarm(
        requester.getId().getValue().toString(),
        community.getId().getValue().toString(),
        community.getName(),
        trigger
    );
    alarmContextManager.send(alarm);
    return this;
  }

  public PaymentRequestReject end() {
    return context;
  }
}
