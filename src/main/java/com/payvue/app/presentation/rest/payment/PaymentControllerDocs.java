package com.payvue.app.presentation.rest.payment;

import com.payvue.app.domain.payment.PaymentProvider;
import com.payvue.app.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import com.payvue.generic.payment.PaymentModeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payment", description = "Payment endpoints")
public interface PaymentControllerDocs {

  @Operation(summary = "Reserve payment", description = "결제 제공사/모드별 결제 예약")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "결제 예약 성공",
          content = @Content(schema = @Schema(implementation = PaymentReserveResponse.class))
      )
  })
  ApiResponse<PaymentReserveResponse> reserve(
      @AuthenticationPrincipal UserDetails userDetails,
      @Parameter(description = "결제 제공사", example = "naver") @PathVariable String provider,
      @Parameter(description = "결제 모드", example = "single") @PathVariable String mode,
      @RequestBody PaymentReserveRequest request
  );

  @Operation(summary = "Confirm payment (redirect callback)", description = "결제창/리디렉트 이후 결제 확정")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "결제 확정 성공",
          content = @Content(schema = @Schema(implementation = Map.class))
      )
  })
  ResponseEntity<String> confirm(
      @Parameter(description = "결제 ID(프로바이더에서 반환된 paymentId/reserveId 등)", example = "pay_123")
      @PathVariable String payId,
      @RequestParam String resultCode,
      @RequestParam String reserveId,
      @RequestParam String paymentId
  );
}
