package com.payvue.app.domain.paymentrequest;

import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;

import java.util.List;
import java.util.Optional;

public interface PaymentRequestRepository {
  PaymentRequest save(PaymentRequest request);
  Optional<PaymentRequest> findById(PaymentRequestId id);
  List<PaymentRequest> findByCommunity(Community community);
  List<PaymentRequest> findByRequester(User user);
  List<PaymentRequest> findByRequesterAndCommunity(User user, Community community);
}
