package com.payvue.app.application.summary.category.balance;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.summary.BalancesByCategory;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;

public final class SummaryAccumulate {
  private final UserDetails userDetails;
  private final Set<AssetClassification> categories;
  private final Map<CommunityCategory, Set<Community>> communities =
      new EnumMap<>(CommunityCategory.class);
  private final Map<CommunityCategory, BalancesByCategory> summaries =
      new EnumMap<>(CommunityCategory.class);
  private User user;
  private BalancesByCategory result;

  public SummaryAccumulate(UserDetails userDetails, Set<AssetClassification> categories) {
    this.userDetails = userDetails;
    this.categories = categories;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public Set<AssetClassification> categories() {
    return categories;
  }

  public Map<CommunityCategory, Set<Community>> communities() {
    return communities;
  }

  public Map<CommunityCategory, BalancesByCategory> summaries() {
    return summaries;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public BalancesByCategory result() {
    return result;
  }

  public void result(BalancesByCategory result) {
    this.result = result;
  }
}
