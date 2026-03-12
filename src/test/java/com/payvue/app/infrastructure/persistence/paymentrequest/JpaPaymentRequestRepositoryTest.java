package com.payvue.app.infrastructure.persistence.paymentrequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestStatus;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaPaymentRequestRepository.class)
class JpaPaymentRequestRepositoryTest {

  @Autowired
  private JpaPaymentRequestRepository repository;

  @Autowired
  private PaymentRequestJpaRepository jpaRepository;

  @Test
  @DisplayName("요청을 저장하고 조회한다")
  void savesAndFindsRequest() {
    PaymentRequest request = PaymentRequest.reconstitute(
        PaymentRequestId.generate(),
        "group-1",
        "user-1",
        1000L,
        "desc",
        "category",
        "high",
        PaymentRequestStatus.PENDING,
        Instant.parse("2024-01-01T00:00:00Z"),
        Instant.parse("2024-01-02T00:00:00Z"),
        Instant.parse("2024-01-03T00:00:00Z"),
        "reason"
    );

    repository.save(request);

    PaymentRequest found = repository.findById(request.id()).orElseThrow();

    assertThat(found.id().value()).isEqualTo(request.id().value());
  }

  @Test
  @DisplayName("그룹과 요청자 기준으로 조회한다")
  void findsByGroupAndRequester() {
    PaymentRequest request = PaymentRequest.reconstitute(
        PaymentRequestId.generate(),
        "group-1",
        "user-1",
        1000L,
        "desc",
        "category",
        "high",
        PaymentRequestStatus.PENDING,
        Instant.parse("2024-01-01T00:00:00Z"),
        Instant.parse("2024-01-02T00:00:00Z"),
        Instant.parse("2024-01-03T00:00:00Z"),
        "reason"
    );

    jpaRepository.save(PaymentRequestEntity.from(request));

    List<PaymentRequestEntity> byGroup = jpaRepository.findByGroupId("group-1");
    List<PaymentRequestEntity> byRequester = jpaRepository.findByRequesterId("user-1");

    assertThat(byGroup).hasSize(1);
    assertThat(byRequester).hasSize(1);
  }
}
