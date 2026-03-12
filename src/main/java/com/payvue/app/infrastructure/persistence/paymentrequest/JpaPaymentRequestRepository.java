package com.payvue.app.infrastructure.persistence.paymentrequest;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaPaymentRequestRepository implements PaymentRequestRepository {

  private final PaymentRequestJpaRepository jpaRepository;

  @Override
  @Transactional
  public PaymentRequest save(PaymentRequest request) {
    PaymentRequestEntity saved = jpaRepository.save(PaymentRequestEntity.from(request));
    return saved.toDomain();
  }

  @Override
  public Optional<PaymentRequest> findById(PaymentRequestId id) {
    if (id == null) {
      return Optional.empty();
    }
    return jpaRepository.findById(id.value()).map(PaymentRequestEntity::toDomain);
  }

  @Override
  public List<PaymentRequest> findByCommunity(Community community) {
    if (community == null || community.getId() == null) {
      return List.of();
    }
    return jpaRepository.findByGroupId(community.getId().toString()).stream()
        .map(PaymentRequestEntity::toDomain)
        .toList();
  }

  @Override
  public List<PaymentRequest> findByRequester(User user) {
    if (user == null || user.getId() == null) {
      return List.of();
    }
    return jpaRepository.findByRequesterId(user.getId().toString()).stream()
        .map(PaymentRequestEntity::toDomain)
        .toList();
  }

  @Override
  public List<PaymentRequest> findByRequesterAndCommunity(User user, Community community) {
    if (user == null || user.getId() == null || community == null || community.getId() == null) {
      return List.of();
    }
    return jpaRepository.findByGroupIdAndRequesterId(
        community.getId().toString(),
        user.getId().toString()
    ).stream()
        .map(PaymentRequestEntity::toDomain)
        .toList();
  }
}
