package com.payvue.app.presentation.rest.hold;

import com.payvue.app.application.asset.AssetApplicationService;
import com.payvue.app.application.summary.SummaryApplicationService;
import com.payvue.app.domain.summary.BalancesByCategory;
import com.payvue.app.domain.community.RecipientType;
import com.payvue.app.presentation.rest.ApiResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.payvue.app.presentation.rest.ResponseCode;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.community.CommunityId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController implements AssetControllerDocs {

  private final SummaryApplicationService summaryApplicationService;
  private final AssetApplicationService assetApplicationService;

  @GetMapping("/summary/online")
  public ApiResponse<List<AssetOnlineItemResponse>> onlineSummary(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(name = "recipientType", defaultValue = "USER") String recipientType,
      @RequestParam(name = "communityId", required = false) String communityId
  ) {
    List<AssetOnlineItemResponse> response = switch (RecipientType.valueOf(recipientType.toUpperCase())) {
      case COMMUNITY -> assetApplicationService.online(user, parseCommunityId(communityId));
      case USER -> assetApplicationService.online(user);
    };
    return ApiResponse.of(ResponseCode.SUCCESS, response);
  }

  @GetMapping("/summary")
  public ApiResponse<AssetSummaryResponse> summary(@AuthenticationPrincipal UserDetails user) {
    BalancesByCategory balancesByCategory = summaryApplicationService.accumulate(user, Set.of(
            AssetClassification.game(),
            AssetClassification.music(),
            AssetClassification.gift(),
            AssetClassification.shopping()
    ));
    AssetSummaryResponse response = AssetSummaryResponse.from(balancesByCategory);
    return ApiResponse.of(ResponseCode.SUCCESS, response);
  }

  @GetMapping("/payment/methods")
  public ApiResponse<List<PaymentMethodResponse>> paymentMethods(
      @AuthenticationPrincipal UserDetails user
  ) {
    return ApiResponse.success(assetApplicationService.paymentMethods(user));
  }

  @PostMapping("/online")
  public ApiResponse<AssetOnlineCreateResponse> createOnlineAsset(
      @AuthenticationPrincipal UserDetails user,
      @Valid @RequestBody AssetOnlineCreateRequest request
  ) {
    String assetId = assetApplicationService.createOnlineAsset(user, request);
    return ApiResponse.success(new AssetOnlineCreateResponse(assetId));
  }

  private CommunityId parseCommunityId(String communityId) {
    if (communityId == null || communityId.isBlank()) {
      throw new IllegalArgumentException("communityId는 필수입니다.");
    }
    return CommunityId.of(UUID.fromString(communityId));
  }
}
