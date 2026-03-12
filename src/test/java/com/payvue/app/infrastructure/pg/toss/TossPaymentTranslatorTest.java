package com.payvue.app.infrastructure.pg.toss;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentProviderResponse;
import com.payvue.generic.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TossPaymentTranslatorTest {

  private final TossPaymentTranslator translator = new TossPaymentTranslator();

  @Test
  @DisplayName("가상계좌 응답은 상태에 따라 결제 상태로 변환된다")
  void translatesVirtualAccountResponse() {
    TossVirtualAccountResponse.VirtualAccount account = new TossVirtualAccountResponse.VirtualAccount(
        "123",
        "088",
        "Alice",
        "2024-01-01"
    );
    TossVirtualAccountResponse response = new TossVirtualAccountResponse(
        "READY",
        "pay-1",
        "order-1",
        1000,
        account
    );

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.metadata()).containsEntry("bankCode", "088");
    assertThat(payment.metadata()).containsEntry("accountNumber", "123");
  }

  @Test
  @DisplayName("Toss 결제 응답은 DONE이면 APPROVED로 변환된다")
  void translatesTossPaymentResponse() {
    VirtualAccountTossGateway.TossPaymentResponse response = new VirtualAccountTossGateway.TossPaymentResponse(
        "DONE",
        "pay-1",
        "order-1",
        1500,
        "CARD"
    );

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(payment.metadata()).containsEntry("method", "CARD");
  }

  @Test
  @DisplayName("Toss 결제 응답이 CANCELED이면 CANCELED로 변환된다")
  void translatesCanceledPaymentResponse() {
    VirtualAccountTossGateway.TossPaymentResponse response = new VirtualAccountTossGateway.TossPaymentResponse(
        "CANCELED",
        "pay-1",
        "order-1",
        1500,
        "CARD"
    );

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(payment.metadata()).containsEntry("status", "CANCELED");
  }

  @Test
  @DisplayName("응답이 null이면 null을 반환한다")
  void returnsNullForNullResponse() {
    Payment payment = translator.translate(null);

    assertThat(payment).isNull();
  }

  @Test
  @DisplayName("알 수 없는 응답 형식은 FAILED로 변환된다")
  void translatesUnknownResponseAsFailed() {
    PaymentProviderResponse response = new PaymentProviderResponse() {};

    Payment payment = translator.translate(response);

    assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.metadata()).containsEntry("message", "지원하지 않는 Toss 응답 형식");
  }
}
