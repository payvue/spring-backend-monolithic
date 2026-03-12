package com.payvue.app.infrastructure.persistence.transaction;

import com.payvue.common.status.ExecutionState;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

  @Id
  private String id;

  @Enumerated(EnumType.STRING)
  private TransactionType type;

  @Column(nullable = false, length = 50)
  private String assetType;

  @Column(nullable = false)
  private String communityId;

  @Column(name = "from_community_id")
  private String fromCommunityId;

  @Column(name = "to_community_id")
  private String toCommunityId;

  @Column(nullable = false)
  private String assetItemId;

  @Column(nullable = false)
  private String actorId;

  @Column(nullable = false)
  private Long amount;

  private String description;

  private String category;

  @Convert(converter = TransactionMetaConverter.class)
  @Column(columnDefinition = "TEXT")
  private TransactionMeta meta;

  @Enumerated(EnumType.STRING)
  private ExecutionState interactionState;

  private Instant occurredAt;

  protected TransactionEntity() {}

  public TransactionEntity(
      String id,
      TransactionType type,
      String assetType,
      String communityId,
      String fromCommunityId,
      String toCommunityId,
      String assetItemId,
      String actorId,
      Long amount,
      String description,
      String category,
      TransactionMeta meta,
      ExecutionState interactionState,
      Instant occurredAt
  ) {
    this.id = id;
    this.type = type;
    this.assetType = assetType;
    this.communityId = communityId;
    this.fromCommunityId = fromCommunityId;
    this.toCommunityId = toCommunityId;
    this.assetItemId = assetItemId;
    this.actorId = actorId;
    this.amount = amount;
    this.description = description;
    this.category = category;
    this.meta = meta;
    this.interactionState = interactionState;
    this.occurredAt = occurredAt;
  }

  public String getId() { return id; }
  public TransactionType getType() { return type; }
  public String getAssetType() { return assetType; }
  public String getCommunityId() { return communityId; }
  public String getFromCommunityId() { return fromCommunityId; }
  public String getToCommunityId() { return toCommunityId; }
  public String getAssetItemId() { return assetItemId; }
  public String getActorId() { return actorId; }
  public Long getAmount() { return amount; }
  public String getDescription() { return description; }
  public String getCategory() { return category; }
  public TransactionMeta getMeta() { return meta; }
  public ExecutionState getInteractionState() { return interactionState; }
  public Instant getOccurredAt() { return occurredAt; }
}
