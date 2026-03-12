package com.payvue.app.infrastructure.persistence.paymentrequest;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentRequestEntityTest {

  @Test
  @DisplayName("PaymentRequest를 Entity로 매핑한다")
  void mapsFromDomain() {
    PaymentRequest request = PaymentRequest.reconstitute(
        PaymentRequestId.generate(),
        "group-1",
        "user-1",
        1000L,
        "desc",
        "category",
        "high",
        PaymentRequestStatus.APPROVED,
        Instant.parse("2024-01-01T00:00:00Z"),
        Instant.parse("2024-01-02T00:00:00Z"),
        Instant.parse("2024-01-03T00:00:00Z"),
        "reason"
    );

    PaymentRequestEntity entity = PaymentRequestEntity.from(request);

    assertThat(entity.toDomain().id().value()).isEqualTo(request.id().value());
    assertThat(entity.toDomain().status()).isEqualTo(PaymentRequestStatus.APPROVED);
  }

  @Test
  @DisplayName("Entity를 PaymentRequest로 매핑한다")
  void mapsToDomain() {
    PaymentRequestEntity entity = PaymentRequestEntity.from(
        PaymentRequest.reconstitute(
            PaymentRequestId.generate(),
            "group-1",
            "user-1",
            1000L,
            "desc",
            "category",
            "high",
            PaymentRequestStatus.REJECTED,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-02T00:00:00Z"),
            Instant.parse("2024-01-03T00:00:00Z"),
            "reason"
        )
    );

    PaymentRequest domain = entity.toDomain();

    assertThat(domain.status()).isEqualTo(PaymentRequestStatus.REJECTED);
    assertThat(domain.rejectionReason()).isEqualTo("reason");
  }

  @Test
  @DisplayName("rejectionReason이 없으면 null로 유지된다")
  void mapsToDomainWithoutRejectionReason() {
    PaymentRequestEntity entity = PaymentRequestEntity.from(
        PaymentRequest.reconstitute(
            PaymentRequestId.generate(),
            "group-1",
            "user-1",
            1000L,
            "desc",
            "category",
            "high",
            PaymentRequestStatus.PENDING,
            Instant.parse("2024-01-01T00:00:00Z"),
            null,
            null,
            null
        )
    );

    PaymentRequest domain = entity.toDomain();

    assertThat(domain.status()).isEqualTo(PaymentRequestStatus.PENDING);
    assertThat(domain.rejectionReason()).isNull();
  }
}
