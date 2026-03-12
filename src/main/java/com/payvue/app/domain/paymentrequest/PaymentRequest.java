package com.payvue.app.domain.paymentrequest;

import java.time.Instant;
import java.util.Objects;

public final class PaymentRequest {

  private final PaymentRequestId id;
  private final String groupId;
  private final String requesterId;
  private final long amount;
  private final String description;
  private final String category;
  private final String urgency;
  private PaymentRequestStatus status;
  private final Instant requestDate;
  private Instant approvedDate;
  private Instant rejectedDate;
  private String rejectionReason;

  private PaymentRequest(
      PaymentRequestId id,
      String groupId,
      String requesterId,
      long amount,
      String description,
      String category,
      String urgency,
      PaymentRequestStatus status,
      Instant requestDate
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
  }

  public static PaymentRequest create(
      String groupId,
      String requesterId,
      long amount,
      String description,
      String category,
      String urgency,
      Instant requestDate
  ) {
    return new PaymentRequest(
        PaymentRequestId.generate(),
        groupId,
        requesterId,
        amount,
        description,
        category,
        urgency,
        PaymentRequestStatus.PENDING,
        requestDate
    );
  }

  public static PaymentRequest createWithId(
      PaymentRequestId id,
      String groupId,
      String requesterId,
      long amount,
      String description,
      String category,
      String urgency,
      Instant requestDate
  ) {
    return new PaymentRequest(
        id,
        groupId,
        requesterId,
        amount,
        description,
        category,
        urgency,
        PaymentRequestStatus.PENDING,
        requestDate
    );
  }

  public static PaymentRequest reconstitute(
      PaymentRequestId id,
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
    PaymentRequest request = new PaymentRequest(
        id,
        groupId,
        requesterId,
        amount,
        description,
        category,
        urgency,
        status,
        requestDate
    );
    request.approvedDate = approvedDate;
    request.rejectedDate = rejectedDate;
    request.rejectionReason = rejectionReason;
    return request;
  }

  public void approve(Instant approvedAt) {
    if (status != PaymentRequestStatus.PENDING) {
      throw new IllegalStateException("승인할 수 없는 상태입니다: " + status);
    }
    this.status = PaymentRequestStatus.APPROVED;
    this.approvedDate = Objects.requireNonNull(approvedAt, "approvedAt은 필수입니다.");
  }

  public void reject(String reason, Instant rejectedAt) {
    if (status != PaymentRequestStatus.PENDING) {
      throw new IllegalStateException("거부할 수 없는 상태입니다: " + status);
    }
    this.status = PaymentRequestStatus.REJECTED;
    this.rejectionReason = Objects.requireNonNull(reason, "reason은 필수입니다.");
    this.rejectedDate = Objects.requireNonNull(rejectedAt, "rejectedAt은 필수입니다.");
  }

  public PaymentRequestId id() { return id; }
  public String groupId() { return groupId; }
  public String requesterId() { return requesterId; }
  public long amount() { return amount; }
  public String description() { return description; }
  public String category() { return category; }
  public String urgency() { return urgency; }
  public PaymentRequestStatus status() { return status; }
  public Instant requestDate() { return requestDate; }
  public Instant approvedDate() { return approvedDate; }
  public Instant rejectedDate() { return rejectedDate; }
  public String rejectionReason() { return rejectionReason; }
}
