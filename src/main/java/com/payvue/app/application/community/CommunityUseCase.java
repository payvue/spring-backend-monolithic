package com.payvue.app.application.community;

import com.payvue.app.application.community.create.CommunityCreateCommand;
import com.payvue.app.presentation.rest.community.CommunityParticipatingResponse;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public interface CommunityUseCase {
  CommunityParticipatingResponse participating(UserDetails userDetails, Boolean standAlone);

  String create(UserDetails userDetails, Boolean standAlone, CommunityCreateCommand command);
  void invite(UserDetails userDetails, UUID id, List<String> emails);
}
