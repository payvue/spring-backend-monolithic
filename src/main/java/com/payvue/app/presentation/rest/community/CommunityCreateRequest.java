package com.payvue.app.presentation.rest.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CommunityCreateRequest(
    @NotBlank
    @Size(max = 30)
    @Schema(description = "그룹 이름", example = "가족 계정")
    String name,
    @Size(max = 100)
    @Schema(description = "그룹 설명", example = "가족 커뮤니티")
    String description,
    @PositiveOrZero
    @Schema(description = "초기 충전 금액", example = "10000")
    Long initialAmount,
    @Schema(description = "초대 멤버 이메일 목록")
    List<@NotBlank @Email String> invitedMembers
) {}
