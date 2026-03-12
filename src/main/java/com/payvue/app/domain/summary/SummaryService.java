package com.payvue.app.domain.summary;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.community.Community;
import java.util.Set;

public interface SummaryService<S extends SummaryView> {
  S summarize(Set<Community> communities, Set<AssetClassification> categories);
  S summarize(Community community, Set<AssetClassification> categories);
  S summarize(Community community, AssetClassification category);
}
