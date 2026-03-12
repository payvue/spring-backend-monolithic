package com.payvue.app.infrastructure.pg.naver;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.infrastructure.pg.PaymentPgProperties;
import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaverPaymentTranslatorTest {

  private final NaverPaymentTranslator translator = new NaverPaymentTranslator(properties());

  @Test
  @DisplayName("Naver 예약 응답은 PENDING 결제로 변환된다")
  void translatesReserveResponse() {
    NaverReserveResponse.Body body = new NaverReserveResponse.Body("reserve-1");
    NaverReserveResponse response = new NaverReserveResponse("Success", "ok", body, "order-1");

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.paymentModeType()).isEqualTo(PaymentModeType.SINGLE);
    assertThat(payment.externalTransactionId()).isEqualTo("reserve-1");
    assertThat(payment.metadata()).containsEntry("redirectUrl", "https://naver.example/reserve-1");
  }

  @Test
  @DisplayName("Naver 승인 응답은 admissionState에 따라 매핑된다")
  void translatesApplyResponseStatus() {
    NaverApplyResponse success = new NaverApplyResponse("Success", "ok", "pid", "SUCCESS");
    NaverApplyResponse fail = new NaverApplyResponse("Success", "ok", "pid", "FAIL");
    NaverApplyResponse pending = new NaverApplyResponse("Success", "ok", "pid", null);

    assertThat(translator.translate(success).status()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(translator.translate(fail).status()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(translator.translate(pending).status()).isEqualTo(PaymentStatus.APPROVED);
  }

  @Test
  @DisplayName("Naver 승인 응답이 실패 코드면 FAILED로 변환된다")
  void translatesApplyResponseFailureCode() {
    NaverApplyResponse response = new NaverApplyResponse("Fail", "bad", "pid", "FAIL");

    assertThat(translator.translate(response).status()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("Naver 취소 응답은 성공 코드면 CANCELED로 변환된다")
  void translatesCancelResponse() {
    NaverCancelResponse success = new NaverCancelResponse("Success", "ok", "pid");
    NaverCancelResponse fail = new NaverCancelResponse("Fail", "bad", "pid");

    assertThat(translator.translate(success).status()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(translator.translate(fail).status()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("Naver 취소 응답에 코드가 없으면 FAILED로 변환된다")
  void translatesCancelResponseWithoutCode() {
    NaverCancelResponse response = new NaverCancelResponse(null, "bad", "pid");

    assertThat(translator.translate(response).status()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("Naver 이력 응답은 entry 수를 메타데이터로 포함한다")
  void translatesHistoryResponse() {
    NaverHistoryResponse.NaverHistoryEntry entry = new NaverHistoryResponse.NaverHistoryEntry(
        "pid",
        "SUCCESS",
        "type",
        "2024-01-01"
    );
    NaverHistoryResponse response = new NaverHistoryResponse("Success", "ok", java.util.List.of(entry));

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.metadata()).containsEntry("historyCount", "1");
  }

  @Test
  @DisplayName("Naver 예약 응답에 body가 없으면 redirectUrl이 비어 있다")
  void translatesReserveResponseWithoutBody() {
    NaverReserveResponse response = new NaverReserveResponse("Success", "ok", null, "order-1");

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.metadata()).doesNotContainKey("redirectUrl");
    assertThat(payment.externalTransactionId()).isNull();
  }

  private static PaymentPgProperties properties() {
    PaymentPgProperties properties = new PaymentPgProperties();
    PaymentPgProperties.Naver naver = new PaymentPgProperties.Naver();
    naver.setPaymentWindowBaseUrl("https://naver.example");
    properties.setNaver(naver);
    return properties;
  }
}
