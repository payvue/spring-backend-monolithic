package com.payvue.app.infrastructure.persistence.transaction;

import com.payvue.core.transaction.command.meta.TransactionMeta;
import com.payvue.core.transaction.command.meta.TransactionMetaKey;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.query.TransactionHistoryView;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
class TransactionHistoryViewMapper {

  public TransactionHistoryView toView(
      TransactionEntity entity,
      String viewerCommunityId,
      String paidBy
  ) {
    TransactionMeta meta = entity.getMeta();
    String merchantId = meta == null ? null : meta.get(TransactionMetaKey.MERCHANT_ID);
    String paymentMethodId = meta == null ? null : meta.get(TransactionMetaKey.PAYMENT_METHOD_ID);
    String platform = meta == null ? null : meta.get(TransactionMetaKey.PLATFORM);
    String location = meta == null ? null : meta.get(TransactionMetaKey.LOCATION);
    String message = meta == null ? null : meta.get(TransactionMetaKey.MESSAGE);

    String status = entity.getInteractionState() == null
        ? null
        : entity.getInteractionState().name().toLowerCase();

    String occurredAt = entity.getOccurredAt() == null ? null : entity.getOccurredAt().toString();
    String time = entity.getOccurredAt() == null
        ? null
        : DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(entity.getOccurredAt());

    Long signedAmount = resolveSignedAmount(entity, viewerCommunityId);
    String kind = resolveKind(entity, viewerCommunityId);
    TransactionHistoryView.CoreInfo core = new TransactionHistoryView.CoreInfo(
        entity.getId(),
        kind,
        signedAmount,
        entity.getDescription(),
        message,
        paidBy,
        occurredAt,
        status,
        entity.getCategory()
    );
    TransactionHistoryView.PaymentInfo payment = new TransactionHistoryView.PaymentInfo(
        merchantId,
        paymentMethodId
    );
    TransactionHistoryView.ContextInfo context = new TransactionHistoryView.ContextInfo(
        platform,
        location,
        time
    );

    TransactionHistoryView.FeeInfo fee = new TransactionHistoryView.FeeInfo(0L);
    if (meta.contains(TransactionMetaKey.COMMISSION)) {
      BigDecimal commission = new BigDecimal(meta.get(TransactionMetaKey.COMMISSION))
          .setScale(0, RoundingMode.HALF_UP);
      fee = new TransactionHistoryView.FeeInfo(commission.longValue());
    }

    return new TransactionHistoryView(core, payment, context, fee);
  }

  private Long resolveSignedAmount(TransactionEntity entity, String viewerCommunityId) {
    Long amount = entity.getAmount();
    if (amount == null || viewerCommunityId == null || viewerCommunityId.isBlank()) {
      return amount;
    }
    String fromId = normalizeId(entity.getFromCommunityId());
    String toId = normalizeId(entity.getToCommunityId());
    String legacyId = normalizeId(entity.getCommunityId());
    String viewerId = normalizeId(viewerCommunityId);

    if (viewerId != null && viewerId.equals(fromId)) {
      return -amount;
    }
    if (viewerId != null && viewerId.equals(toId)) {
      return amount;
    }
    if (viewerId != null && viewerId.equals(legacyId)) {
      return -amount;
    }
    return amount;
  }

  private String resolveKind(TransactionEntity entity, String viewerCommunityId) {
    if (entity == null || entity.getType() == null) {
      return null;
    }
    String toId = normalizeId(entity.getToCommunityId());
    String legacyId = normalizeId(entity.getCommunityId());
    String viewerId = normalizeId(viewerCommunityId);
    if (viewerId != null
        && (viewerId.equals(toId) || (toId == null && viewerId.equals(legacyId)))
        && entity.getType() == TransactionType.TRANSFER) {
      return "acquire";
    }
    return entity.getType().name().toLowerCase();
  }

  private String normalizeId(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.toLowerCase(java.util.Locale.ROOT);
  }
}
