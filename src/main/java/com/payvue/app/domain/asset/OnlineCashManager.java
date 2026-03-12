package com.payvue.app.domain.asset;

import com.payvue.core.asset.AssetClassification;
import com.payvue.core.community.Community;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;

public interface OnlineCashManager {
  void initial(Community community, User owner, long initialAmount);
  void credit(Community community, User owner, long amount);
  Money possesses(Community community);
  Money possesses(Community community, AssetClassification category);
}
