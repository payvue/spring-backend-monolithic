package com.payvue.app.presentation.rest.community;

import com.payvue.app.presentation.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Community", description = "Community endpoints")
public interface CommunityControllerDocs {

  @Operation(summary = "Participating communities", description = "List participating communities")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Participating communities")
  })
  ApiResponse<CommunityParticipatingResponse> participating(
      @AuthenticationPrincipal UserDetails user,
      @Parameter(description = "개인 커뮤니티 여부", example = "false")
      boolean standAlone
  );

  @Operation(summary = "Create community", description = "Create new community")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Community created")
  })
  ApiResponse<CommunityCreateResponse> create(
      @AuthenticationPrincipal UserDetails user,
      @RequestBody CommunityCreateRequest request,
      @Parameter(description = "개인 커뮤니티 여부", example = "false")
      boolean standAlone
  );
}
