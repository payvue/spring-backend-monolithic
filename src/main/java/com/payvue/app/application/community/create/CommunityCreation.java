package com.payvue.app.application.community.create;

import com.payvue.app.domain.community.Personal;
import com.payvue.core.community.Community;
import com.payvue.shared.Money;
import com.payvue.supporting.user.command.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityCreation {
  private final UserDetails userDetails;
  private final boolean standAlone;
  private final CommunityCreateCommand command;
  private final List<User> invited = new ArrayList<>();
  private final List<User> successfulInvitees = new ArrayList<>();
  private User user;
  private Personal personalCommunity;
  private Community community;

  public CommunityCreation(UserDetails userDetails, boolean standAlone, CommunityCreateCommand command) {
    this.userDetails = userDetails;
    this.standAlone = standAlone;
    this.command = command;
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public boolean standAlone() {
    return standAlone;
  }

  public CommunityCreateCommand command() {
    return command;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public Personal personalCommunity() {
    return personalCommunity;
  }

  public void personalCommunity(Personal personalCommunity) {
    this.personalCommunity = personalCommunity;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }

  public List<User> invited() {
    return invited;
  }

  public void addInvitee(User user) {
    if (user != null) {
      invited.add(user);
    }
  }

  public List<User> successfulInvitees() {
    return successfulInvitees;
  }

  public void addSuccessfulInvitee(User user) {
    if (user != null) {
      successfulInvitees.add(user);
    }
  }
}
