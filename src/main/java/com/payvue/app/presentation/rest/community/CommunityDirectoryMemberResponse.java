package com.payvue.app.presentation.rest.community;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityDirectoryMemberResponse(
    @Schema(description = "멤버 ID", example = "member-jihoon")
    String id,
    @Schema(description = "멤버 이름", example = "김지훈")
    String name,
    @Schema(description = "역할", example = "운영진")
    String role,
    @Schema(description = "아바타 URL", example = "https://i.pravatar.cc/150?img=12")
    String avatar
) {}
