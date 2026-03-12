package com.payvue.app.application.asset.create;

import com.payvue.app.presentation.rest.hold.AssetOnlineCreateRequest;
import com.payvue.core.asset.AssetItem;
import com.payvue.core.community.Community;
import com.payvue.core.hold.Holdings;
import com.payvue.supporting.user.command.User;
import org.springframework.security.core.userdetails.UserDetails;

public final class OnlineAssetCreateContext {
  private final UserDetails userDetails;
  private final AssetOnlineCreateRequest request;
  private User user;
  private Community community;
  private Holdings holdings;
  private AssetItem asset;

  OnlineAssetCreateContext(UserDetails userDetails, AssetOnlineCreateRequest request) {
    this.userDetails = userDetails;
    this.request = request;
  }

  UserDetails userDetails() { return userDetails; }
  AssetOnlineCreateRequest request() { return request; }

  User user() { return user; }
  void user(User user) { this.user = user; }

  Community community() { return community; }
  void community(Community community) { this.community = community; }

  Holdings holdings() { return holdings; }
  void holdings(Holdings holdings) { this.holdings = holdings; }

  AssetItem asset() { return asset; }
  void asset(AssetItem asset) { this.asset = asset; }

  public String assetId() { return asset == null ? null : asset.getId().toString(); }
}
