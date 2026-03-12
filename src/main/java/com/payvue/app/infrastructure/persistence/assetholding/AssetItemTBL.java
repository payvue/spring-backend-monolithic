package com.payvue.app.infrastructure.persistence.assetholding;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "asset_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetItemTBL {

  @Id
  @Column(name = "asset_item_id", columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "amount")
  private Integer amount;

  @Column(name = "category", nullable = false, length = 50)
  private String category;

  @Column(name = "status", length = 20)
  private String status;

  @Column(name = "owner_user_id", columnDefinition = "BINARY(16)")
  private UUID ownerUserId;

  @Column(name = "owner_community_id", columnDefinition = "BINARY(16)")
  private UUID ownerCommunityId;

  @OneToOne(mappedBy = "assetItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private AssetItemCapabilityTBL capability;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public AssetItemTBL(
      Integer amount,
      String name,
      String category,
      String status,
      UUID ownerUserId,
      UUID ownerCommunityId
  ) {
    this.id = UUID.randomUUID();
    this.amount = amount;
    this.name = name;
    this.category = category;
    this.status = status;
    this.ownerUserId = ownerUserId;
    this.ownerCommunityId = ownerCommunityId;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public static AssetItemTBL create(
      Integer amount,
      String name,
      String category,
      String status,
      UUID ownerUserId,
      UUID ownerCommunityId
  ) {
    return new AssetItemTBL(
        amount,
        name,
        category,
        status,
        ownerUserId,
        ownerCommunityId
    );
  }

  public void setCapability(AssetItemCapabilityTBL capability) {
    this.capability = capability;
    if (capability != null && capability.getAssetItem() != this) {
      capability.setAssetItem(this);
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
