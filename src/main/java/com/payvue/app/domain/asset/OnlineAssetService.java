package com.payvue.app.domain.asset;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetStatus;
import com.payvue.core.community.Community;
import java.util.Set;

public interface OnlineAssetService {
  Set<AssetItem> listOnlineItems(Community community);
  Set<AssetItem> listOnlineItems(Community community, AssetClassification category);
  Set<AssetItem> listOnlineItems(Community community, AssetStatus status);
  Set<AssetItem> listOnlineItems(Community community, AssetClassification category, AssetStatus status);
}
