package com.payvue.app.infrastructure.persistence.paymentrequest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRequestJpaRepository extends JpaRepository<PaymentRequestEntity, UUID> {

  List<PaymentRequestEntity> findByGroupId(String groupId);

  List<PaymentRequestEntity> findByRequesterId(String requesterId);

  List<PaymentRequestEntity> findByGroupIdAndRequesterId(String groupId, String requesterId);
}
