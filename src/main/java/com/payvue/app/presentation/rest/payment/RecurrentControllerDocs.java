package com.payvue.app.presentation.rest.payment;

import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Recurrent", description = "Recurrent payment endpoints")
public interface RecurrentControllerDocs {

  @Operation(summary = "Auto charge conditions", description = "List auto charge strategies")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Auto charge strategies")
  })
  ApiResponse<List<RecurrentAutoChargeResponse>> chargeConditions();

  @Operation(summary = "Scheduled payments", description = "List scheduled payment conditions")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled payments")
  })
  ApiResponse<List<RecurrentScheduledPaymentResponse>> scheduledConditions();
}
