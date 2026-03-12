package com.payvue.app.application.community;

import com.payvue.app.application.community.create.CommunityCreateCommand;
import com.payvue.app.application.community.create.CommunityCreation;
import com.payvue.app.application.community.create.CommunityCreationFlow;
import com.payvue.app.application.community.invite.CommunityInvitationFlow;
import com.payvue.app.application.community.participate.CommunityParticipationFlow;
import com.payvue.app.application.transaction.TransactionUseCase;
import com.payvue.app.domain.asset.OnlineCashManager;
import com.payvue.app.domain.community.MemberManager;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.app.presentation.rest.community.CommunityParticipatingResponse;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityRepository;
import com.payvue.generic.alarm.AlarmContextManager;
import com.payvue.supporting.interaction.InteractionExecutor;
import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityApplicationService implements CommunityUseCase {

  private final CommunityRepository communityRepository;
  private final CommunityContextManager communityContextManager;
  private final RoleCatalog roleCatalog;
  private final UserContextManager userContextManager;
  private final InteractionExecutor interactionExecutor;
  private final MemberManager memberManager;
  private final OnlineCashManager onlineCashManager;
  private final TransactionUseCase transactionUseCase;
  private final AlarmContextManager alarmContextManager;

  @Override
  @Transactional(readOnly = true)
  public CommunityParticipatingResponse participating(UserDetails userDetails, Boolean standAlone) {
    boolean standaloneFlag = standAlone != null && standAlone;
    return CommunityParticipationFlow.start(userDetails, standaloneFlag)
        .user(step -> step.loadUser(userContextManager))
        .translate(CommunityParticipationFlow::resolveCommunityIds)
        .community(step -> step.loadCommunities(communityContextManager))
        .map(step -> step.buildUserViews(userContextManager))
        .asset(step -> step.loadBalances(onlineCashManager))
        .result(CommunityParticipationFlow::toResponse)
        .end();
  }

  @Override
  @Transactional
  public String create(UserDetails userDetails, Boolean standAlone, CommunityCreateCommand command) {
    boolean standaloneFlag = standAlone != null && standAlone;
    CommunityCreation context = CommunityCreationFlow.start(userDetails, standaloneFlag, command)
        .community(step -> step.validate(communityRepository))
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.createCommunity(communityContextManager))
        .transaction(step -> step.transferCash(transactionUseCase))
        .user(step -> step.loadInvitees(userContextManager))
        .interaction(step -> step.inviteMembers(interactionExecutor, roleCatalog))
        .notify(step -> step.sendInvites(alarmContextManager))
        .save(step -> step.registerOwnerMembership(userContextManager))
        .end();

    return context.community().getId().toString();
  }

  @Override
  public void invite(UserDetails userDetails, UUID id, List<String> emails) {
    CommunityInvitationFlow.start(userDetails, id, emails)
        .community(step -> step.loadCommunity(communityRepository))
        .user(step -> step.loadInitiator(userContextManager).loadInvitees(userContextManager))
        .interaction(step -> step.invite(memberManager))
        .notify(step -> step.sendInvites(alarmContextManager))
        .save(step -> step.persist(communityRepository))
        .end();
  }

}
