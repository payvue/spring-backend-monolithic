package com.payvue.app.infrastructure.persistence.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentId;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentStatus;
import com.payvue.generic.payment.ProviderId;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaPaymentRepository.class)
class JpaPaymentRepositoryTest {

  @Autowired
  private JpaPaymentRepository repository;

  @Autowired
  private PaymentJpaRepository paymentJpaRepository;

  @Test
  @DisplayName("결제를 저장하고 조회한다")
  void savesAndFindsPayment() {
    Payment payment = Payment.reconstitute(
        PaymentId.generate(),
        PaymentStatus.PENDING,
        PaymentModeType.SINGLE,
        new ProviderId("NAVER"),
        "external",
        Map.of("key", "value")
    );

    repository.save(payment);

    Payment found = repository.findById(payment.getId());

    assertThat(found.getId()).isEqualTo(payment.getId());
    assertThat(found.status()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  @DisplayName("없는 결제는 예외를 던진다")
  void throwsWhenPaymentMissing() {
    PaymentId missingId = PaymentId.generate();

    assertThatThrownBy(() -> repository.findById(missingId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("결제를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("JPA repository는 Entity를 저장한다")
  void jpaRepositoryStoresEntity() {
    PaymentEntity entity = new PaymentEntity(
        PaymentId.generate(),
        PaymentStatus.PENDING,
        PaymentModeType.SINGLE,
        new ProviderId("NAVER"),
        "external",
        Map.of("key", "value")
    );

    paymentJpaRepository.save(entity);

    assertThat(paymentJpaRepository.findById(entity.getId())).isPresent();
  }
}
