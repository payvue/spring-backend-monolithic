package com.payvue.app.application.alarm.invite;

import com.payvue.app.domain.alarm.GroupAlarmContent;
import com.payvue.app.infrastructure.persistence.alarm.AlarmEntity;
import com.payvue.core.community.Community;
import com.payvue.supporting.user.command.User;

public final class AlarmInvitationHandle {
  private final String alarmId;
  private final String action;
  private final String readerEmail;
  private AlarmEntity alarm;
  private User user;
  private GroupAlarmContent content;
  private Community community;

  public AlarmInvitationHandle(String alarmId, String action, String readerEmail) {
    this.alarmId = alarmId;
    this.action = action;
    this.readerEmail = readerEmail;
  }

  public String alarmId() {
    return alarmId;
  }

  public String action() {
    return action;
  }

  public String readerEmail() {
    return readerEmail;
  }

  public AlarmEntity alarm() {
    return alarm;
  }

  public void alarm(AlarmEntity alarm) {
    this.alarm = alarm;
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public GroupAlarmContent content() {
    return content;
  }

  public void content(GroupAlarmContent content) {
    this.content = content;
  }

  public Community community() {
    return community;
  }

  public void community(Community community) {
    this.community = community;
  }
}
