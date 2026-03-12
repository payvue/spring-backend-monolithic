package com.payvue.app.application.community.invite;

import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

public final class CommunityInvitation {
  private final UserDetails userDetails;
  private final UUID communityId;
  private final List<String> emails;
  private final List<User> invitees = new ArrayList<>();
  private final List<User> successfulInvitees = new ArrayList<>();
  private User initiator;
  private Community community;

  public CommunityInvitation(UserDetails userDetails, UUID communityId, List<String> emails) {
    this.userDetails = userDetails;
    this.communityId = communityId;
    this.emails = emails == null ? List.of() : List.copyOf(emails);
  }

  public UserDetails userDetails() {
    return userDetails;
  }

  public UUID communityId() {
    return communityId;
  }

  public List<String> emails() {
    return emails;
  }

  public List<User> invitees() {
    return invitees;
  }

  public void addInvitee(User user) {
    if (user != null) {
      invitees.add(user);
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

  public User initiator() {
    return initiator;
  }

  public void initiator(User initiator) {
    this.initiator = initiator;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }
}
