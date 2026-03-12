package com.payvue.app.infrastructure.pg.naver;

import com.payvue.app.infrastructure.pg.PaymentPgProperties;
import com.payvue.generic.payment.*;
import com.payvue.app.domain.payment.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NaverPaymentTranslator implements PaymentTranslator<NaverPaymentResponse> {

  private final PaymentPgProperties properties;

  @Override
  public ProviderId providerId() {
    return new ProviderId(PaymentProvider.NAVER.name());
  }

  @Override
  public Payment translate(NaverPaymentResponse response) {

    if (response instanceof NaverReserveResponse reserveResponse) {
      String reserveId = reserveResponse.body() != null ? reserveResponse.body().reserveId() : null;
      Map<String, String> metadata = new java.util.HashMap<>();
      if (reserveId != null) {
        metadata.put("reserveId", reserveId);
        String baseUrl = properties.getNaver().getPaymentWindowBaseUrl();
        metadata.put("redirectUrl", baseUrl.endsWith("/") ? baseUrl + reserveId : baseUrl + "/" + metadata.get("reserveId"));
      }
      if (reserveResponse.code() != null) metadata.put("code", reserveResponse.code());
      if (reserveResponse.message() != null) metadata.put("message", reserveResponse.message());

      return Payment.of(
          PaymentId.of(reserveResponse.orderId()),
          PaymentStatus.PENDING,
          PaymentModeType.SINGLE,
          providerId(),
          reserveId,
          metadata
      );
    }
    if (response instanceof NaverApplyResponse applyResponse) {
      Map<String, String> metadata = new java.util.HashMap<>();
      if (applyResponse.admissionState() != null) metadata.put("admissionState", applyResponse.admissionState());
      if (applyResponse.code() != null) metadata.put("code", applyResponse.code());
      if (applyResponse.message() != null) metadata.put("message", applyResponse.message());

      return Payment.of(
          applyResponse.code().equals("Success") ? PaymentStatus.APPROVED : PaymentStatus.FAILED,
          PaymentModeType.SINGLE,
          providerId(),
          applyResponse.paymentId(),
          metadata
      );
    }
    if (response instanceof NaverCancelResponse cancelResponse) {
      Map<String, String> metadata = new java.util.HashMap<>();
      if (cancelResponse.code() != null) metadata.put("code", cancelResponse.code());
      if (cancelResponse.message() != null) metadata.put("message", cancelResponse.message());

      return Payment.of(
          mapCancelStatus(cancelResponse),
          PaymentModeType.SINGLE,
          providerId(),
          cancelResponse.paymentId(),
          metadata
      );
    }
    if (response instanceof NaverHistoryResponse historyResponse) {
      int size = historyResponse.entries() == null ? 0 : historyResponse.entries().size();
      Map<String, String> metadata = new java.util.HashMap<>();
      metadata.put("historyCount", String.valueOf(size));
      if (historyResponse.code() != null) metadata.put("code", historyResponse.code());
      if (historyResponse.message() != null) metadata.put("message", historyResponse.message());

      return Payment.of(
          PaymentStatus.PENDING,
          PaymentModeType.SINGLE,
          providerId(),
          null,
          metadata
      );
    }
    return Payment.of(
        PaymentStatus.FAILED,
        PaymentModeType.SINGLE,
        providerId(),
        null,
        Map.of("message", "지원하지 않는 응답 유형입니다.")
    );
  }

  private static PaymentStatus mapCancelStatus(
      NaverCancelResponse response
  ) {
    if (response.code() == null) {
      return PaymentStatus.FAILED;
    }
    return "Success".equalsIgnoreCase(response.code())
        ? PaymentStatus.CANCELED
        : PaymentStatus.FAILED;
  }
}
