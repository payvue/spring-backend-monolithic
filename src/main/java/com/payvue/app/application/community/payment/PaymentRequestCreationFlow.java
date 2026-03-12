package com.payvue.app.application.community.payment;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.presentation.rest.community.PaymentRequestCreateRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.core.community.Community;
import com.payvue.core.community.CategoryQuery;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.Participant;
import com.payvue.app.domain.alarm.InAppAlarm;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.alarm.AlarmFactory;
import com.payvue.generic.alarm.Trigger;
import com.payvue.supporting.interaction.CreatePaymentRequestDecision;
import com.payvue.supporting.interaction.Interaction;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.interaction.PaymentRequestCreateIntent;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class PaymentRequestCreationFlow {
  private final PaymentRequestCreation context;

  private PaymentRequestCreationFlow(PaymentRequestCreation context) {
    this.context = context;
  }

  public static PaymentRequestCreationFlow start(
      UserDetails userDetails,
      UUID communityId,
      PaymentRequestCreateRequest request
  ) {
    return new PaymentRequestCreationFlow(new PaymentRequestCreation(userDetails, communityId, request));
  }

  public PaymentRequestCreationFlow user(Consumer<PaymentRequestCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestCreationFlow community(Consumer<PaymentRequestCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestCreationFlow request(Consumer<PaymentRequestCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestCreationFlow map(Consumer<PaymentRequestCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestCreationFlow notify(Consumer<PaymentRequestCreationFlow> step) {
    step.accept(this);
    return this;
  }

  public PaymentRequestCreationFlow loadRequester(UserContextManager userContextManager) {
    User requester = userContextManager
        .findByEmail(context.userDetails().getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.requester(requester);
    return this;
  }

  public PaymentRequestCreationFlow loadCommunity(
      CommunityContextManager communityContextManager,
      CommunityCategory category
  ) {
    Community community = communityContextManager.get(
        new CategoryQuery(
            category.name(),
            CommunityId.of(context.communityId()))
        );
    context.community(community);
    return this;
  }

  public PaymentRequestCreationFlow createRequest(InteractionExecutor interactionExecutor) {
    PaymentRequestCreateRequest request = context.request();
    Instant now = Instant.now();
    context.createdAt(now);

    PaymentRequest paymentRequest = PaymentRequest.create(
        context.community().getId().toString(),
        context.requester().getId().toString(),
        request.amount(),
        request.description(),
        request.category(),
        request.urgency(),
        now
    );
    context.paymentRequest(paymentRequest);

    Participant initiator = Community.to(
        context.community(),
        context.requester().getId().getValue()
    );
    Interaction interaction = interactionExecutor.executeInternal(
        new CreatePaymentRequestDecision(
            new PaymentRequestCreateIntent(
                initiator,
                paymentRequest.id().toString(),
                context.community().getId().toString(),
                request.amount(),
                request.description(),
                request.category(),
                request.urgency(),
                now
            )
        ),
        context.community()
    );
    if (interaction.isFailed()) {
      throw new IllegalStateException("요청 생성에 실패했습니다.");
    }
    return this;
  }

  public PaymentRequestCreationFlow sendAlarm(AlarmContextManager alarmContextManager) {
    User requester = context.requester();
    Community community = context.community();
    PaymentRequest request = context.paymentRequest();
    if (requester == null || community == null || request == null) {
      return this;
    }
    UserAccountView requesterView = UserAccountView.from(requester);
    Trigger trigger = new Trigger(
        requesterView.displayName(),
        community.getParticipantRole(requester.getId().getValue()).getName()
    );
    for (UUID participantId : community.getParticipantIds()) {
      if (!community.isAdmin(participantId)) {
        continue;
      }
      if (participantId.equals(requester.getId().getValue())) {
        continue;
      }
      InAppAlarm alarm = AlarmFactory.createGroupPaymentRequestAlarm(
          participantId.toString(),
          community.getId().getValue().toString(),
          community.getName(),
          trigger,
          request.amount(),
          community.getName()
      );
      alarmContextManager.send(alarm);
    }
    return this;
  }

  public PaymentRequestResponse end() {
    String requesterName = UserAccountView.from(context.requester()).displayName();
    return PaymentRequestResponse.from(context.paymentRequest(), requesterName);
  }
}
