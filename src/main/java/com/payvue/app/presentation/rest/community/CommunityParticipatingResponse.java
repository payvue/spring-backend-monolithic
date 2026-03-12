package com.payvue.app.presentation.rest.community;

import com.payvue.common.status.EngagementState;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CommunityParticipatingResponse(
    @Schema(description = "이슈 존재 여부", example = "true")
    boolean hasIssue,
    @Schema(description = "커뮤니티 그룹 목록")
    List<Group> groups
) {

  public record Group(
      @Schema(description = "그룹 ID", example = "1")
      String id,
      @Schema(description = "그룹 이름", example = "가족 계정")
      String name,
      @Schema(description = "멤버 목록")
      List<Member> members,
      @Schema(description = "공유 정보")
      Shared shared
  ) {}

  public record Member(
      @Schema(description = "멤버 ID", example = "f1")
      String id,
      @Schema(description = "역할", example = "leader")
      String role,
      @Schema(description = "상태 정보")
      Status status,
      @Schema(description = "프로필 정보")
      Info info,
      @Schema(description = "기여 정보")
      Contribution contribution,
      @Schema(description = "정책 정보")
      Policy policy
  ) {}

  public record Status(
      @Schema(description = "로그인 여부", example = "true")
      boolean isLoggedIn,
      @Schema(description = "마지막 로그인", example = "2025-09-30T07:45:00Z")
      String lastLoggedIn
  ) {}

  public record Info(
      @Schema(description = "이름", example = "테스트 계정")
      String name,
      @Schema(description = "연락처")
      Contact contact
  ) {}

  public record Contact(
      @Schema(description = "이메일", example = "dad@family.com")
      String email,
      @Schema(description = "전화번호", example = "01012341234")
      String phone
  ) {}

  public record Contribution(
      @Schema(description = "기여 금액", example = "12000")
      long amount,
      @Schema(description = "참여 날짜", example = "2025-09-30T07:45:00Z")
      String joinDate
  ) {}

  public record Policy(
      @Schema(description = "활성화 여부", example = "true")
      boolean active
  ) {}

  public record Shared(
      @Schema(description = "공유 잔액", example = "150000")
      long balance
  ) {}

  public static CommunityParticipatingResponse from(
      List<Community> communities,
      Map<UserId, UserAccountView> userViews,
      Map<CommunityId, Long> balances,
      UserId currentUserId,
      boolean standAlone
  ) {
    if (communities == null || communities.isEmpty()) {
      return new CommunityParticipatingResponse(false, List.of());
    }
    List<Group> groups = new ArrayList<>();
    for (Community community : communities) {
      if (!matchesType(community, standAlone)) {
        continue;
      }
      groups.add(toGroup(community, userViews, balances, currentUserId));
    }
    return new CommunityParticipatingResponse(false, groups);
  }

  private static Group toGroup(
      Community community,
      Map<UserId, UserAccountView> userViews,
      Map<CommunityId, Long> balances,
      UserId currentUserId
  ) {
    List<Member> members = new ArrayList<>();
    for (UUID participantId : community.getParticipantIds()) {
      RoleDescriptor role = community.getParticipantRole(participantId);
      UserId userId = UserId.of(participantId);
      members.add(toMember(community, userId, role, userViews, currentUserId));
    }
    long balance = 0L;
    if (balances != null && community != null) {
      Long value = balances.get(community.getId());
      if (value != null) {
        balance = value;
      }
    }
    Shared shared = new Shared(balance);
    return new Group(
        community.getId().toString(),
        safeCommunityName(community),
        members,
        shared
    );
  }

  private static Member toMember(
      Community community,
      UserId participantId,
      RoleDescriptor role,
      Map<UserId, UserAccountView> userViews,
      UserId currentUserId
  ) {
    CommunityId communityId = community == null ? null : community.getId();
    UserAccountView view = userViews == null
        ? emptyView()
        : userViews.getOrDefault(participantId, emptyView());
    Status status = new Status(view.loggedIn(), toString(view.lastLoggedIn()));
    Contact contact = new Contact(view.email(), view.phoneNumber());
    Info info = new Info(view.displayName(), contact);
    Instant joinedAtValue = currentUserId != null && currentUserId.equals(participantId)
        ? view.joinFor(communityId == null ? null : communityId.getValue())
        : null;
    Contribution contribution = new Contribution(0L, toString(joinedAtValue));
    Policy policy = new Policy(isActiveParticipant(community, participantId));
    return new Member(
        participantId.getValue().toString(),
        role == null ? null : role.getName(),
        status,
        info,
        contribution,
        policy
    );
  }

  private static String toString(Instant value) {
    return value == null ? null : value.toString();
  }

  private static boolean isActiveParticipant(Community community, UserId participantId) {
    if (community == null || participantId == null) {
      return false;
    }
    try {
      EngagementState state = community.getParticipantState(participantId.getValue());
      return state != null && state.canParticipate();
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private static String safeCommunityName(Community community) {
    if (community == null) {
      return null;
    }
    try {
      return community.getName();
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static boolean matchesType(Community community, boolean standAlone) {
    if (community == null) {
      return false;
    }
    String type = community.getCommunityType();
    if (type == null) {
      return true;
    }
    if (standAlone) {
      return "PERSONAL".equalsIgnoreCase(type);
    }
    return "COOPERATIVE".equalsIgnoreCase(type);
  }

  private static UserAccountView emptyView() {
    return new UserAccountView(
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        false,
        null,
        Map.of()
    );
  }
}
