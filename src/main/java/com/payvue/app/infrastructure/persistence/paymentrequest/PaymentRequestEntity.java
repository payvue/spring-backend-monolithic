package com.payvue.app.infrastructure.persistence.paymentrequest;

import com.payvue.app.domain.paymentrequest.PaymentRequest;
import com.payvue.app.domain.paymentrequest.PaymentRequestId;
import com.payvue.app.domain.paymentrequest.PaymentRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_requests")
public class PaymentRequestEntity {

  @Id
  @Column(name = "payment_request_id", nullable = false)
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private String groupId;

  @Column(name = "requester_id", nullable = false)
  private String requesterId;

  @Column(nullable = false)
  private long amount;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String urgency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentRequestStatus status;

  @Column(name = "requested_at", nullable = false)
  private Instant requestDate;

  @Column(name = "approved_at")
  private Instant approvedDate;

  @Column(name = "rejected_at")
  private Instant rejectedDate;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  protected PaymentRequestEntity() {
    // for JPA
  }

  private PaymentRequestEntity(
      UUID id,
      String groupId,
      String requesterId,
      long amount,
      String description,
      String category,
      String urgency,
      PaymentRequestStatus status,
      Instant requestDate,
      Instant approvedDate,
      Instant rejectedDate,
      String rejectionReason
  ) {
    this.id = Objects.requireNonNull(id, "id는 필수입니다.");
    this.groupId = Objects.requireNonNull(groupId, "groupId는 필수입니다.");
    this.requesterId = Objects.requireNonNull(requesterId, "requesterId는 필수입니다.");
    this.amount = amount;
    this.description = Objects.requireNonNull(description, "description은 필수입니다.");
    this.category = Objects.requireNonNull(category, "category는 필수입니다.");
    this.urgency = Objects.requireNonNull(urgency, "urgency는 필수입니다.");
    this.status = Objects.requireNonNull(status, "status는 필수입니다.");
    this.requestDate = Objects.requireNonNull(requestDate, "requestDate는 필수입니다.");
    this.approvedDate = approvedDate;
    this.rejectedDate = rejectedDate;
    this.rejectionReason = rejectionReason;
  }

  public static PaymentRequestEntity from(PaymentRequest request) {
    return new PaymentRequestEntity(
        request.id().value(),
        request.groupId(),
        request.requesterId(),
        request.amount(),
        request.description(),
        request.category(),
        request.urgency(),
        request.status(),
        request.requestDate(),
        request.approvedDate(),
        request.rejectedDate(),
        request.rejectionReason()
    );
  }

  public PaymentRequest toDomain() {
    return PaymentRequest.reconstitute(
        PaymentRequestId.of(id),
        groupId,
        requesterId,
        amount,
        description,
        category,
        urgency,
        status,
        requestDate,
        approvedDate,
        rejectedDate,
        rejectionReason
    );
  }
}
