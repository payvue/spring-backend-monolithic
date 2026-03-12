package com.payvue.app.presentation.rest.transaction;

import com.payvue.app.application.transaction.TransactionUseCase;
import com.payvue.app.application.transaction.command.TransferAssetToCommunityCommand;
import com.payvue.app.application.transaction.command.TransferAssetToUserCommand;
import com.payvue.app.application.transaction.command.TransferCashCommand;
import com.payvue.app.domain.community.RecipientType;
import com.payvue.app.application.transaction.result.TransferAssetResult;
import com.payvue.app.application.transaction.result.TransferCashResult;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.core.transaction.command.AssetType;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
class TransactionController {

    private final TransactionUseCase transactionUseCase;

    @PostMapping("/transfer/cash")
    public ApiResponse<TransferCashResponse> transferCash(
      @AuthenticationPrincipal UserDetails user,
      @jakarta.validation.Valid @RequestBody TransferCashRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {

    TransferCashCommand command = new TransferCashCommand(
        request.recipientId(),
        request.recipientType() == null ? RecipientType.COMMUNITY : request.recipientType(),
        request.amount(),
        request.currency(),
        request.message(),
        request.clientRequestId()
        );
        TransferCashResult result = transactionUseCase.transferCash(user, command, idempotencyKey);
        return ApiResponse.success(new TransferCashResponse(
            result.transferId(),
            result.status(),
            result.balance()
        ));
    }

    @PostMapping("/transfer/user/{assetType}")
    public ApiResponse<TransferAssetResponse> transferAssetToUser(
        @AuthenticationPrincipal UserDetails user,
        @PathVariable String assetType,
        @jakarta.validation.Valid @RequestBody TransferAssetToUserRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
      TransferAssetToUserCommand command = new TransferAssetToUserCommand(
          request.assetId(),
          request.recipientEmail(),
          request.message(),
          request.clientRequestId()
      );
      TransferAssetResult result = transactionUseCase.transferAssetToUser(
          user,
          AssetType.valueOf(assetType.toUpperCase(Locale.ROOT)),
          command,
          idempotencyKey
      );
      return ApiResponse.success(
          new TransferAssetResponse(result.transferId(), result.status())
      );
    }

    @PostMapping("/transfer/community/{assetType}")
    public ApiResponse<TransferAssetResponse> transferAssetToCommunity(
        @AuthenticationPrincipal UserDetails user,
        @PathVariable String assetType,
        @jakarta.validation.Valid @RequestBody TransferAssetToCommunityRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
      TransferAssetToCommunityCommand command = new TransferAssetToCommunityCommand(
          request.assetId(),
          request.groupId(),
          request.message(),
          request.clientRequestId()
      );
      TransferAssetResult result = transactionUseCase.transferAssetToCommunity(
          user,
          AssetType.valueOf(assetType.toUpperCase(Locale.ROOT)),
          command,
          idempotencyKey
      );
      return ApiResponse.success(
          new TransferAssetResponse(result.transferId(), result.status())
      );
    }

}
