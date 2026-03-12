package com.payvue.app.application.alarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.application.alarm.invite.AlarmInvitationHandleFlow;
import com.payvue.app.infrastructure.persistence.alarm.AlarmJpaRepository;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmActionService {

  private final AlarmJpaRepository alarmJpaRepository;
  private final AlarmReadService alarmReadService;
  private final CommunityContextManager communityContextManager;
  private final UserContextManager userContextManager;
  private final ObjectMapper objectMapper;
  private final AlarmContextManager alarmContextManager;

  @Transactional
  public void handleInvitation(String alarmId, String action, String readerEmail) {
    AlarmInvitationHandleFlow.start(alarmId, action, readerEmail)
        .alarm(step -> step.loadAlarm(alarmJpaRepository, objectMapper))
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadCommunity(communityContextManager))
        .action(step -> step.applyAction(communityContextManager, userContextManager, alarmReadService))
        .notify(step -> step.sendAlarm(alarmContextManager))
        .end();
  }
}
