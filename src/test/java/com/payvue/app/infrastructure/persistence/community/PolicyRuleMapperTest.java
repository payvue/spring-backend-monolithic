package com.payvue.app.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.app.domain.community.rules.IsParticipantRule;
import com.payvue.app.domain.community.rules.MaxMembersRule;
import com.payvue.core.community.policy.Rule;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyRuleMapperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PolicyRuleMapper mapper = new PolicyRuleMapper(objectMapper);

  @Test
  @DisplayName("MaxMembersRule을 정책 엔티티로 변환한다")
  void mapsMaxMembersRuleToEntity() throws Exception {
    CommunityTBL community = PersonalTBL.create("Personal", "desc");
    MaxMembersRule rule = new MaxMembersRule(5);

    PolicyRuleTBL entity = mapper.toEntity(community, rule, 1);

    assertThat(entity.getRuleType()).isEqualTo(MaxMembersRule.class.getSimpleName());
    Map<String, Object> config = objectMapper.readValue(entity.getRuleConfig(), Map.class);
    assertThat(config.get("maxMembers")).isEqualTo(5);
  }

  @Test
  @DisplayName("IsParticipantRule을 도메인으로 복원한다")
  void mapsIsParticipantRuleToDomain() {
    UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    PolicyRuleTBL entity = PolicyRuleTBL.create(
        PersonalTBL.create("Personal", "desc"),
        IsParticipantRule.class.getSimpleName(),
        "{\"userId\":\"" + userId + "\"}",
        1
    );

    Rule rule = mapper.toDomain(entity);

    assertThat(rule).isInstanceOf(IsParticipantRule.class);
    assertThat(((IsParticipantRule) rule).getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("HasDecisionAuthorityRule을 도메인으로 복원한다")
  void mapsDecisionAuthorityRuleToDomain() {
    UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    PolicyRuleTBL entity = PolicyRuleTBL.create(
        PersonalTBL.create("Personal", "desc"),
        HasDecisionAuthorityRule.class.getSimpleName(),
        "{\"userId\":\"" + userId + "\"}",
        1
    );

    Rule rule = mapper.toDomain(entity);

    assertThat(rule).isInstanceOf(HasDecisionAuthorityRule.class);
    assertThat(readField(rule, "userId", UUID.class)).isEqualTo(userId);
  }

  @Test
  @DisplayName("HasDecisionAuthorityRule을 정책 엔티티로 변환한다")
  void mapsDecisionAuthorityRuleToEntity() throws Exception {
    UUID userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    HasDecisionAuthorityRule rule = new HasDecisionAuthorityRule(userId);

    PolicyRuleTBL entity = mapper.toEntity(PersonalTBL.create("Personal", "desc"), rule, 2);

    Map<String, Object> config = objectMapper.readValue(entity.getRuleConfig(), Map.class);
    assertThat(config.get("userId")).isEqualTo(userId.toString());
  }

  private static <T> T readField(Object target, String name, Class<T> type) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      Object value = field.get(target);
      return type.cast(value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read field: " + name, e);
    }
  }
}
