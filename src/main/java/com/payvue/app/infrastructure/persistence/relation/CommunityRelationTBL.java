package com.payvue.app.infrastructure.persistence.relation;


import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "community_relation",
    indexes = {
      @Index(
          name = "idx_from_relation_type",
          columnList = "from_community_id, relation_type"
      )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityRelationTBL {

  @Id
  @Column(name = "relation_id", columnDefinition = "BINARY(16)", nullable = false)
  private UUID id;

  @Column(name = "from_community_id", columnDefinition = "BINARY(16)", nullable = false)
  private UUID fromCommunityId;

  @Column(name = "to_community_id", columnDefinition = "BINARY(16)", nullable = false)
  private UUID toCommunityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "relation_type", nullable = false, length = 20)
  private RelationType relationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConnectionStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  CommunityRelationTBL(
      UUID id,
      UUID fromCommunityId,
      UUID toCommunityId,
      RelationType relationType,
      ConnectionStatus status
  ) {
    this.id = id;
    this.fromCommunityId = fromCommunityId;
    this.toCommunityId = toCommunityId;
    this.relationType = relationType;
    this.status = status;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public static CommunityRelationTBL create(
      UUID id,
      UUID fromCommunityId,
      UUID toCommunityId,
      RelationType relationType,
      ConnectionStatus status
  ) {
    return new CommunityRelationTBL(
        id,
        fromCommunityId,
        toCommunityId,
        relationType,
        status
    );
  }

  public void updateStatus(ConnectionStatus newStatus) {
    this.status = newStatus;
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
