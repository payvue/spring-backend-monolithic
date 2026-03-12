package com.payvue.app.application.community;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.presentation.rest.community.PaymentRequestApproveRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestCreateRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestRejectRequest;
import com.payvue.app.presentation.rest.community.PaymentRequestResponse;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

public interface PaymentRequestUseCase {

  PaymentRequestResponse create(
      UserDetails user,
      UUID communityId,
      CommunityCategory category,
      PaymentRequestCreateRequest request
  );

  List<PaymentRequestResponse> list(
      UserDetails user,
      UUID communityId,
      CommunityCategory category
  );

  List<PaymentRequestResponse> listMy(UserDetails user);

  List<PaymentRequestResponse> listMyByCommunity(
      UserDetails user,
      UUID communityId,
      CommunityCategory category
  );

  void approve(
      UserDetails user,
      String requestId,
      CommunityCategory category,
      PaymentRequestApproveRequest request
  );

  void reject(
      UserDetails user,
      String requestId,
      CommunityCategory category,
      PaymentRequestRejectRequest request
  );
}
