package com.payvue.app.infrastructure.persistence.payment;

import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentId;
import com.payvue.generic.payment.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaPaymentRepository implements PaymentRepository {

  private final PaymentJpaRepository paymentJpaRepository;

  @Override
  @Transactional
  public void save(Payment payment) {
    paymentJpaRepository.save(PaymentEntity.from(payment));
  }

  @Override
  public Payment findById(PaymentId id) {
    Optional<PaymentEntity> entity = paymentJpaRepository.findById(id);
    return entity.map(PaymentEntity::toDomain)
        .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다."));
  }
}
