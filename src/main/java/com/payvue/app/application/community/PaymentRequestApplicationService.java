package com.payvue.app.application.community;

import com.payvue.app.application.community.payment.PaymentRequestApproveFlow;
import com.payvue.app.application.community.payment.PaymentRequestCreationFlow;
import com.payvue.app.application.community.payment.PaymentRequestListingFlow;
import com.payvue.app.application.community.payment.MyPaymentRequestListFlow;
import com.payvue.app.application.community.payment.PaymentRequestRejectFlow;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.app.presentation.rest.community.PaymentRequestApproveRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestCreateRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestRejectRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.user.UserContextManager;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRequestApplicationService implements PaymentRequestUseCase {

  private final UserContextManager userContextManager;
  private final CommunityContextManager communityContextManager;
  private final InteractionExecutor interactionExecutor;
  private final AlarmContextManager alarmContextManager;

  private final PaymentRequestRepository paymentRequestRepository;

  @Override
  @Transactional
  public PaymentRequestResponse create(
      UserDetails user,
      UUID communityId,
      CommunityCategory category,
      PaymentRequestCreateRequest request
  ) {
    return PaymentRequestCreationFlow.start(user, communityId, request)
        .user(step -> step.loadRequester(userContextManager))
        .community(step -> step.loadCommunity(communityContextManager, category))
        .request(step -> step.createRequest(interactionExecutor))
        .notify(step -> step.sendAlarm(alarmContextManager))
        .end();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentRequestResponse> list(
      UserDetails user,
      UUID communityId,
      CommunityCategory category
  ) {
    return PaymentRequestListingFlow.start(communityId)
        .community(step -> step.loadCommunity(communityContextManager, category))
        .request(step -> step.loadByCommunity(paymentRequestRepository))
        .map(step -> step.toResponses(userContextManager))
        .end();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentRequestResponse> listMy(UserDetails user) {
    return MyPaymentRequestListFlow.start(user)
        .user(step -> step.loadRequester(userContextManager))
        .request(step -> step.loadByRequester(paymentRequestRepository))
        .map(step -> step.toResponses(userContextManager))
        .end();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentRequestResponse> listMyByCommunity(
      UserDetails user,
      UUID communityId,
      CommunityCategory category
  ) {
    return MyPaymentRequestListFlow.start(user)
        .user(step -> step.loadRequester(userContextManager))
        .community(step -> step.loadCommunity(communityContextManager, category, communityId))
        .request(step -> step.loadByRequesterAndCommunity(paymentRequestRepository))
        .map(step -> step.toResponses(userContextManager))
        .end();
  }

  @Override
  @Transactional
  public void approve(
      UserDetails user,
      String requestId,
      CommunityCategory category,
      PaymentRequestApproveRequest request
  ) {
    PaymentRequestApproveFlow.start(user, requestId, request)
        .request(step -> step.loadRequest(paymentRequestRepository))
        .community(step -> step.loadCommunity(communityContextManager, category))
        .user(step -> step.loadRequester(userContextManager).loadApprover(userContextManager))
        .interaction(step -> step.approve(interactionExecutor))
        .notify(step -> step.sendAlarm(alarmContextManager))
        .end();
  }

  @Override
  public void reject(
      UserDetails user,
      String requestId,
      CommunityCategory category,
      PaymentRequestRejectRequest request
  ) {
    PaymentRequestRejectFlow.start(user, requestId, request)
        .request(step -> step.loadRequest(paymentRequestRepository))
        .community(step -> step.loadCommunity(communityContextManager, category))
        .user(step -> step.loadRequester(userContextManager).loadApprover(userContextManager))
        .interaction(step -> step.reject(interactionExecutor))
        .notify(step -> step.sendAlarm(alarmContextManager))
        .end();
  }

}
