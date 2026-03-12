package com.payvue.app.presentation.rest.community;

import com.payvue.app.application.community.CommunityUseCase;
import com.payvue.app.application.community.create.CommunityCreateCommand;
import com.payvue.app.presentation.rest.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController implements CommunityControllerDocs {

  private final CommunityUseCase communityUseCase;

  @GetMapping("/participating")
  public ApiResponse<CommunityParticipatingResponse> participating(
      @AuthenticationPrincipal UserDetails user,
      @RequestParam(name = "standAlone", defaultValue = "false") boolean standAlone
  ) {
    return ApiResponse.success(communityUseCase.participating(user, standAlone));
  }

  @PostMapping("/create")
  public ApiResponse<CommunityCreateResponse> create(
      @AuthenticationPrincipal UserDetails user,
      @Valid @RequestBody CommunityCreateRequest request,
      @RequestParam(name = "standAlone", defaultValue = "false") boolean standAlone
  ) {
    String communityId = communityUseCase.create(user, standAlone, new CommunityCreateCommand(
        request.name(),
        request.description(),
        request.initialAmount(),
        request.invitedMembers()
    ));
    return ApiResponse.success(new CommunityCreateResponse(communityId));
  }

  @PostMapping("/invite/{id}")
  public ApiResponse<Void> invite(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable UUID id,
      @RequestBody List<String> emails
  ) {
    communityUseCase.invite(user, id, emails);
    return ApiResponse.success();
  }
}
