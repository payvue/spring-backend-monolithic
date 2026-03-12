package com.payvue.app.infrastructure.persistence.community;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.core.community.policy.ChainableRule;
import com.payvue.core.community.policy.Rule;
import com.payvue.app.domain.community.rules.HasDecisionAuthorityRule;
import com.payvue.app.domain.community.rules.IsParticipantRule;
import com.payvue.app.domain.community.rules.MaxMembersRule;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PolicyRuleMapper {

  private final ObjectMapper objectMapper;

  public PolicyRuleMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Rule toDomain(PolicyRuleTBL entity) {
    if (entity == null) {
      return null;
    }
    String type = entity.getRuleType();
    Map<String, Object> config = parseConfig(entity.getRuleConfig());

    if (matchesType(type, MaxMembersRule.class)) {
      int maxMembers = ((Number) config.getOrDefault("maxMembers", 0)).intValue();
      return new MaxMembersRule(maxMembers);
    }
    if (matchesType(type, IsParticipantRule.class)) {
      UUID userId = UUID.fromString((String) config.get("userId"));
      return new IsParticipantRule(userId);
    }
    if (matchesType(type, HasDecisionAuthorityRule.class)) {
      UUID userId = UUID.fromString((String) config.get("userId"));
      return new HasDecisionAuthorityRule(userId);
    }

    throw new IllegalArgumentException("Unsupported rule type: " + type);
  }

  public List<Rule> toDomainList(List<PolicyRuleTBL> entities) {
    if (entities == null) {
      return List.of();
    }
    return entities.stream()
      .map(this::toDomain)
      .collect(Collectors.toList());
  }

  public PolicyRuleTBL toEntity(CommunityTBL community, Rule rule, int orderIndex) {
    if (community == null || rule == null) {
      return null;
    }

    String ruleType = rule.getClass().getSimpleName();
    String ruleConfig = writeConfig(rule);
    return PolicyRuleTBL.create(community, ruleType, ruleConfig, orderIndex);
  }

  private boolean matchesType(String value, Class<?> type) {
    if (value == null) {
      return false;
    }
    String simple = type.getSimpleName();
    return value.equals(simple) || value.endsWith("." + simple);
  }

  private Map<String, Object> parseConfig(String config) {
    if (config == null || config.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(config, Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid rule config JSON", e);
    }
  }

  private String writeConfig(Rule rule) {
    Map<String, Object> config = new HashMap<>();

    if (rule instanceof MaxMembersRule) {
      config.put("maxMembers", readField(rule, "maxMembers", Integer.class));
    } else if (rule instanceof IsParticipantRule) {
      UUID userId = ((IsParticipantRule) rule).getUserId();
      config.put("userId", userId.toString());
    } else if (rule instanceof HasDecisionAuthorityRule) {
      UUID userId = readField(rule, "userId", UUID.class);
      config.put("userId", userId.toString());
    } else if (rule instanceof ChainableRule) {
      throw new IllegalArgumentException("Unsupported chainable rule: " + rule.getClass().getSimpleName());
    } else {
      throw new IllegalArgumentException("Unsupported rule: " + rule.getClass().getSimpleName());
    }

    try {
      return objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to write rule config", e);
    }
  }

  private static <T> T readField(Object source, String fieldName, Class<T> type) {
    try {
      Field field = source.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      Object value = field.get(source);
      return type.cast(value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read field: " + fieldName, e);
    }
  }
}
