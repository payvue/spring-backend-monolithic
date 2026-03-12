package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.presentation.rest.community.PaymentRequestApproveRequest;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.Participant;
import com.payvue.supporting.interaction.ApproveDecision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.PaymentRequestIntent;
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

public final class PaymentRequestApproveFlow {
  private final PaymentRequestApprove context;

  private PaymentRequestApproveFlow(PaymentRequestApprove context) {
    this.context = context;
  }

  public static PaymentRequestApproveFlow start(
      UserDetails userDetails,
      String requestId,
      PaymentRequestApproveRequest request
  ) {
    return new PaymentRequestApproveFlow(new PaymentRequestApprove(userDetails, requestId, request));
  }

  public PaymentRequestApproveFlow request(Consumer<PaymentRequestApproveFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestApproveFlow community(Consumer<PaymentRequestApproveFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestApproveFlow user(Consumer<PaymentRequestApproveFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestApproveFlow interaction(Consumer<PaymentRequestApproveFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestApproveFlow notify(Consumer<PaymentRequestApproveFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestApproveFlow loadRequest(PaymentRequestRepository paymentRequestRepository) {
    PaymentRequest paymentRequest = paymentRequestRepository.findById(
        PaymentRequestId.of(UUID.fromString(context.requestId()))
    ).orElseThrow(() -> new IllegalArgumentException("결제 요청을 찾을 수 없습니다."));
    context.paymentRequest(paymentRequest);
    return this;
  }

  public PaymentRequestApproveFlow loadCommunity(
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

  public PaymentRequestApproveFlow loadRequester(UserContextManager userContextManager) {
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

  public PaymentRequestApproveFlow loadApprover(UserContextManager userContextManager) {
    User approver = userContextManager
        .findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.approver(approver);
    return this;
  }

  public PaymentRequestApproveFlow approve(InteractionExecutor interactionExecutor) {
    Participant initiator = Community.to(context.community(), context.approver().getId().getValue());
    Interaction interaction = interactionExecutor.executeInternal(
        new ApproveDecision<>(
            new PaymentRequestIntent(
                initiator,
                context.paymentRequest().id().toString(),
                context.paymentRequest().groupId()
            )
        ),
        context.community()
    );
    if (interaction.isFailed()) {
      throw new IllegalStateException("요청 승인에 실패했습니다.");
    }
    return this;
  }

  public PaymentRequestApproveFlow sendAlarm(AlarmContextManager alarmContextManager) {
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
    InAppAlarm alarm = AlarmFactory.createPaymentRequestApprovedAlarm(
        requester.getId().getValue().toString(),
        community.getId().getValue().toString(),
        community.getName(),
        trigger
    );
    alarmContextManager.send(alarm);
    return this;
  }

  public PaymentRequestApprove end() {
    return context;
  }
}
