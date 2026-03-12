package com.payvue.app.presentation.rest.community;

import com.payvue.app.application.community.PaymentRequestUseCase;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.presentation.rest.ApiResponse;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class PaymentRequestController {

  private final PaymentRequestUseCase paymentRequestUseCase;

  @PostMapping("/{communityId}/payment-request")
  public ApiResponse<PaymentRequestResponse> create(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable UUID communityId,
      @RequestParam(name = "category") String category,
      @RequestBody PaymentRequestCreateRequest request
  ) {
    return ApiResponse.success(paymentRequestUseCase.create(user, communityId, CommunityCategory.valueOf(category.toUpperCase()), request));
  }

  @GetMapping("/{communityId}/payment-requests")
  public ApiResponse<List<PaymentRequestResponse>> list(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable UUID communityId,
      @RequestParam(name = "category") String category
  ) {
    return ApiResponse.success(paymentRequestUseCase.list(user, communityId, CommunityCategory.valueOf(category.toUpperCase())));
  }

  @GetMapping("/payment-requests/my")
  public ApiResponse<List<PaymentRequestResponse>> listMy(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(name = "communityId") UUID communityId,
      @RequestParam(name = "category", required = false) String category
  ) {
    if (communityId == null || category == null || category.isBlank()) {
      return ApiResponse.success(paymentRequestUseCase.listMy(user));
    }
    return ApiResponse.success(paymentRequestUseCase.listMyByCommunity(
        user,
        communityId,
        CommunityCategory.valueOf(category.toUpperCase())
    ));
  }

  @PutMapping("/payment-request/{requestId}/approve")
  public ApiResponse<Void> approve(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable String requestId,
      @RequestParam(name = "communityCategory") String category,
      @RequestBody(required = false) PaymentRequestApproveRequest request
  ) {
    paymentRequestUseCase.approve(user, requestId, CommunityCategory.valueOf(category.toUpperCase()), request);
    return ApiResponse.success();
  }

  @PutMapping("/payment-request/{requestId}/reject")
  public ApiResponse<Void> reject(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable String requestId,
      @RequestParam(name = "category") String category,
      @RequestBody PaymentRequestRejectRequest request
  ) {
    paymentRequestUseCase.reject(user, requestId, CommunityCategory.valueOf(category.toUpperCase()), request);
    return ApiResponse.success();
  }
}
