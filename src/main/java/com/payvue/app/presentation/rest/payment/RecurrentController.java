package com.payvue.app.presentation.rest.payment;

import com.payvue.app.presentation.rest.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recurrent")
public class RecurrentController implements RecurrentControllerDocs {

  @GetMapping("/condition/charge")
  public ApiResponse<List<RecurrentAutoChargeResponse>> chargeConditions() {
    List<RecurrentAutoChargeResponse> responses = List.of(
        new RecurrentAutoChargeResponse(
            "auto-001",
            "잔액 1만원 이하 자동 충전",
            "신한카드",
            true,
            30000,
            new RecurrentAutoChargeResponse.Strategy(
                "condition",
                "balance_below",
                10000,
                "잔액 부족 시 자동 충전"
            )
        ),
        new RecurrentAutoChargeResponse(
            "auto-002",
            "월초 정기 충전",
            "국민은행 (110-***-*****)",
            false,
            100000,
            new RecurrentAutoChargeResponse.Strategy(
                "condition",
                "date_based",
                1,
                "매월 1일 자동 충전"
            )
        )
    );
    return ApiResponse.success(responses);
  }

  @GetMapping("/condition/scheduled")
  public ApiResponse<List<RecurrentScheduledPaymentResponse>> scheduledConditions() {
    List<RecurrentScheduledPaymentResponse> responses = List.of(
        new RecurrentScheduledPaymentResponse(
            "1",
            "넷플릭스 프리미엄",
            "넷플릭스 프리미엄",
            17000,
            "PayEasy 잔액",
            "active",
            "2024-01-15",
            "2024-02-15",
            null,
            new RecurrentScheduledPaymentResponse.Strategy("schedule", "monthly"),
            true,
            "다음 결제: 2024-02-15",
            "2024-01-15"
        ),
        new RecurrentScheduledPaymentResponse(
            "2",
            "스포티파이 프리미엄",
            "스포티파이 프리미엄",
            10900,
            "신한카드 (**** 1234)",
            "failed",
            "2024-01-10",
            "2024-02-10",
            "잔액 부족",
            new RecurrentScheduledPaymentResponse.Strategy("schedule", "monthly"),
            false,
            "다음 결제: 2024-02-10",
            "2024-01-10"
        ),
        new RecurrentScheduledPaymentResponse(
            "3",
            "Adobe Creative Cloud",
            "Adobe Creative Cloud",
            24000,
            "PayEasy 잔액",
            "paused",
            "2024-12-20",
            "2024-02-20",
            null,
            new RecurrentScheduledPaymentResponse.Strategy("schedule", "monthly"),
            false,
            "다음 결제: 2024-02-20",
            "2024-12-20"
        )
    );
    return ApiResponse.success(responses);
  }
}
