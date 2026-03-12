package com.payvue.app.infrastructure.persistence.payment;

import com.payvue.generic.payment.Payment;
import com.payvue.generic.payment.PaymentId;
import com.payvue.generic.payment.PaymentModeType;
import com.payvue.generic.payment.PaymentStatus;
import com.payvue.generic.payment.ProviderId;
import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "payments")
public class PaymentEntity {

  @EmbeddedId
  @AttributeOverride(name = "value", column = @Column(name = "payment_id", length = 36))
  private PaymentId id;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  private PaymentModeType paymentModeType;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "provider_id"))
  private ProviderId providerId;

  private String externalTransactionId;

  @Lob
  @Column(name = "metadata", columnDefinition = "TEXT")
  @Convert(converter = JsonMapConverter.class)
  private Map<String, String> metadata;

  protected PaymentEntity() {
    // for JPA
  }

  public PaymentEntity(
          PaymentId id,
      PaymentStatus status,
      PaymentModeType paymentModeType,
          ProviderId providerId,
      String externalTransactionId,
      Map<String, String> metadata
  ) {
    this.id = id;
    this.status = status;
    this.paymentModeType = paymentModeType;
    this.providerId = providerId;
    this.externalTransactionId = externalTransactionId;
    this.metadata = metadata;
  }

  public PaymentId getId() { return id; }
  public PaymentStatus getStatus() { return status; }
  public PaymentModeType getPaymentModeType() { return paymentModeType; }
  public ProviderId getProviderId() { return providerId; }
  public String getExternalTransactionId() { return externalTransactionId; }
  public Map<String, String> getMetadata() { return metadata; }

  public Payment toDomain() {
      return Payment.reconstitute(id, status, paymentModeType, providerId, externalTransactionId, metadata);
  }

  public static PaymentEntity from(Payment payment) {
    return new PaymentEntity(
        payment.getId(),
        payment.status(),
        payment.paymentModeType(),
        payment.providerId(),
        payment.externalTransactionId(),
        payment.metadata()
    );
  }
}
