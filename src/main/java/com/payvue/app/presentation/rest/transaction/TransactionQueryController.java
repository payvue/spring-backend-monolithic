package com.payvue.app.presentation.rest.transaction;

import com.payvue.app.application.transaction.TransactionQueryUseCase;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.shared.Period;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.payvue.core.community.CommunityId;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transaction/query")
@RequiredArgsConstructor
class TransactionQueryController {

    private final TransactionQueryUseCase transactionQueryUseCase;

    @GetMapping("/personal/asset")
    public ApiResponse<TransactionHistoryResponse> assetTransactions(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody(required = false) TransferHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                TransactionHistoryResponse.fromResult(
                        transactionQueryUseCase.queryPersonal(
                                user,
                                AssetType.ALL.name(),
                                TransactionType.ALL,
                                toPageable(request),
                                toPeriod(request)
                        )
                )
        );
    }

    @GetMapping("/personal/recent")
    public ApiResponse<TransactionHistoryResponse> recentTransactions(
            @AuthenticationPrincipal UserDetails user
    ) {

        return ApiResponse.success(
                TransactionHistoryResponse.fromResult(
                        transactionQueryUseCase.queryPersonal(
                                user,
                                AssetType.ALL.name(),
                                TransactionType.ALL,
                                PageRequest.of(
                                        0,
                                        5,
                                        Sort.by(Sort.Direction.DESC, "occurredAt")
                                ),
                                Period.unbounded()
                        )
                )
        );
    }

    @GetMapping("/community/{id}")
    public ApiResponse<TransactionHistoryResponse> groupTransactions(
            @PathVariable UUID id,
            @RequestBody(required = false) TransferHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                TransactionHistoryResponse.fromResult(
                        transactionQueryUseCase.queryCommunity(
                                CommunityId.of(id),
                                AssetType.ALL.name(),
                                TransactionType.ALL,
                                toPageable(request),
                                toPeriod(request)
                        )
                )
        );
    }

    @GetMapping("/history")
    public ApiResponse<TransactionHistoryResponse> history(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(name = "count", defaultValue = "10") int page,
            @RequestParam(name = "limit", defaultValue = "1") int limit
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit));
        return ApiResponse.success(
                TransactionHistoryResponse.fromResult(
                        transactionQueryUseCase.queryPersonal(
                                user,
                                AssetType.ALL.name(),
                                TransactionType.ALL,
                                pageable,
                                Period.unbounded()
                        )
                )
        );
    }

    @GetMapping("/{assetType}/{txType}")
    public ApiResponse<TransactionHistoryResponse> transferHistory(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String assetType,
            @PathVariable String txType,
            @RequestBody(required = false) TransferHistoryQueryRequest request
    ) {

        return ApiResponse.success(
                TransactionHistoryResponse.fromResult(
                        transactionQueryUseCase.queryPersonal(
                                user,
                                assetType.toUpperCase(Locale.ROOT),
                                TransactionType.valueOf(txType.toUpperCase(Locale.ROOT)),
                                toPageable(request),
                                toPeriod(request)
                        )
                )
        );
    }

    private Pageable toPageable(TransferHistoryQueryRequest request) {
        int page = request == null || request.page() == null ? 1 : request.page();
        int size = request == null || request.size() == null ? 20 : request.size();
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
    }

    private Period toPeriod(TransferHistoryQueryRequest request) {
        if (request == null) {
            return Period.unbounded();
        }
        Instant from = parseInstant(request.fromDate());
        Instant to = parseInstant(request.toDate());
        if (from == null && to == null) {
            return Period.unbounded();
        }
        return Period.of(from, to);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

}
