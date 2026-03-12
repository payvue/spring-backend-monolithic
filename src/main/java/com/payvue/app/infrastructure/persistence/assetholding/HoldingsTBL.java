package com.payvue.app.infrastructure.persistence.assetholding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldingsTBL {

  @Id
  @Column(name = "holdings_id", columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "community_id", columnDefinition = "BINARY(16)", nullable = false)
  private UUID communityId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  HoldingsTBL(UUID id, UUID communityId) {
    this.id = id;
    this.communityId = communityId;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public static HoldingsTBL create(UUID id, UUID communityId) {
    return new HoldingsTBL(id, communityId);
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
