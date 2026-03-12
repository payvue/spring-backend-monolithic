package com.payvue.app.infrastructure.persistence.assetholding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "asset_item_capabilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetItemCapabilityTBL {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(name = "capability_id", columnDefinition = "BINARY(16)")
  private UUID id;

  @OneToOne(optional = false)
  @JoinColumn(name = "asset_item_id", nullable = false, unique = true)
  private AssetItemTBL assetItem;

  @Column(name = "config_json", columnDefinition = "TEXT", nullable = false)
  private String configJson;

  public AssetItemCapabilityTBL(AssetItemTBL assetItem, String configJson) {
    this.assetItem = assetItem;
    this.configJson = configJson;
  }

  public static AssetItemCapabilityTBL create(AssetItemTBL assetItem, String configJson) {
    return new AssetItemCapabilityTBL(assetItem, configJson);
  }

  void setAssetItem(AssetItemTBL assetItem) {
    this.assetItem = assetItem;
  }
}
