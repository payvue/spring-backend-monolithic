package com.payvue.app.application.pay;

import com.payvue.app.application.pay.confirm.PayCompletion;
import com.payvue.app.application.pay.confirm.PayCompletionFlow;
import com.payvue.app.application.pay.reserve.PayReserveContext;
import com.payvue.app.application.pay.reserve.PayReserveFlow;
import com.payvue.app.presentation.rest.community.SystemCommunityProvider;
import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.app.domain.payment.PaymentService;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.core.transaction.command.TransactionService;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.generic.payment.OrderItem;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentRepository;
import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayApplicationService {

  private final PaymentService paymentService;
  private final PaymentRepository paymentRepository;
  private final CommunityContextManager communityContextManager;
  private final UserContextManager userContextManager;
  private final HoldingsRepository holdingsRepository;
  private final TransactionService transactionService;
  private final AlarmContextManager alarmContextManager;
  private final SystemCommunityProvider systemCommunityProvider;

  @Transactional
  public Payment complete(String payId, String resultCode, String reserveId, String paymentId) {
    PayCompletion context = PayCompletionFlow.start(payId, resultCode, reserveId, paymentId)
        .payment(step -> step.confirmPayment(paymentService))
        .community(step -> step.loadCommunity(communityContextManager))
        .user(step -> step.loadOwner(userContextManager))
        .asset(step -> step.loadHoldings(holdingsRepository).rebuildOrderItemFromMetadata())
        .transaction(step -> step.acquire(transactionService, systemCommunityProvider))
        .notify(step -> step.send(alarmContextManager))
        .save(step -> step.persist(paymentRepository))
        .end();

    return context.payment();
  }

  @Transactional
  public Payment reserve(
      UserDetails userDetails,
      PaymentProvider provider,
      PaymentModeType modeType,
      OrderItem order,
      String bankCode
  ) {
    PayReserveContext context = PayReserveFlow.start(userDetails, provider, modeType, order, bankCode)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadPersonalCommunity(communityContextManager))
        .payment(step -> step.reserve(paymentService).enrichMetadata())
        .save(step -> step.persist(paymentRepository))
        .end();

    return context.payment();

  }
}
