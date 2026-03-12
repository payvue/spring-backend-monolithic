package com.payvue.app.infrastructure.persistence.payment;

import com.payvue.generic.payment.PaymentId;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, PaymentId> {
}
