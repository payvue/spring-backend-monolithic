package com.payvue.app.infrastructure.persistence.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentId;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentStatus;
import com.payvue.generic.payment.ProviderId;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentEntityTest {

  @Test
  @DisplayName("Payment에서 Entity로 변환한다")
  void mapsFromDomain() {
    Payment payment = Payment.reconstitute(
        PaymentId.generate(),
        PaymentStatus.PENDING,
        PaymentModeType.SINGLE,
        new ProviderId("NAVER"),
        "external",
        Map.of("key", "value")
    );

    PaymentEntity entity = PaymentEntity.from(payment);

    assertThat(entity.getId()).isEqualTo(payment.getId());
    assertThat(entity.getStatus()).isEqualTo(payment.status());
    assertThat(entity.getProviderId()).isEqualTo(payment.providerId());
    assertThat(entity.getMetadata()).containsEntry("key", "value");
  }

  @Test
  @DisplayName("Entity에서 Payment로 변환한다")
  void mapsToDomain() {
    PaymentId id = PaymentId.generate();
    PaymentEntity entity = new PaymentEntity(
        id,
        PaymentStatus.APPROVED,
        PaymentModeType.SINGLE,
        new ProviderId("NAVER"),
        "external",
        Map.of("key", "value")
    );

    Payment payment = entity.toDomain();

    assertThat(payment.getId()).isEqualTo(id);
    assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(payment.providerId()).isEqualTo(new ProviderId("NAVER"));
  }
}
