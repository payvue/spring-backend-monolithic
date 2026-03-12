package com.payvue.app.presentation.rest.hold;

import com.payvue.app.domain.community.RecipientType;
import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;

@Tag(name = "Asset", description = "Asset endpoints")
public interface AssetControllerDocs {

  @Operation(summary = "Online asset summary", description = "Get online asset summary")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Online assets")
  })
  ApiResponse<List<AssetOnlineItemResponse>> onlineSummary(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(name = "recipientType", defaultValue = "USER") String recipientType,
      @RequestParam(name = "communityId", required = false) String communityId
  );

  @Operation(summary = "Asset summary", description = "Get asset summary")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Asset summary")
  })
  ApiResponse<AssetSummaryResponse> summary(@AuthenticationPrincipal UserDetails user);

  @Operation(summary = "Payment methods", description = "Get payment methods")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment methods")
  })
  ApiResponse<List<PaymentMethodResponse>> paymentMethods(
      @AuthenticationPrincipal UserDetails user
  );

  @Operation(summary = "Create online asset", description = "Create an online asset item")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created asset")
  })
  ApiResponse<AssetOnlineCreateResponse> createOnlineAsset(
      @AuthenticationPrincipal UserDetails user,
      @Valid @RequestBody AssetOnlineCreateRequest request
  );
}
