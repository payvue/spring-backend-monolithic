package com.payvue.app.infrastructure.persistence.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.app.domain.community.Cooperative;
import com.payvue.core.community.Participant;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.app.domain.community.SystemCommunity;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.community.policy.CommunityPolicy;
import com.payvue.common.status.EngagementState;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CommunityEntityMapper {

  @org.springframework.beans.factory.annotation.Autowired
  private RoleCatalog roleCatalog;

  public abstract Community toDomain(CommunityTBL entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "participants", ignore = true)
  @Mapping(target = "policyRules", ignore = true)
  public abstract CommunityTBL toEntity(Community domain);

  protected RoleDescriptor mapRole(String value) {
    if (value == null) {
      return null;
    }
    return roleCatalog.resolve(value);
  }

  @ObjectFactory
  protected Community createCommunity(CommunityTBL entity) {
    if (entity == null) {
      return null;
    }
    UUID adminId = resolveAdminId(entity.getParticipants());
    if (entity instanceof PersonalTBL) {
      return Personal.create(entity.getName(), entity.getDescription(), adminId, roleCatalog.admin());
    }
    if (entity instanceof CooperativeTBL) {
      return Cooperative.create(
          entity.getName(),
          entity.getDescription(),
          adminId,
          roleCatalog.admin(),
          roleCatalog.member()
      );
    }
    if (entity instanceof SystemTBL) {
      return SystemCommunity.create(roleCatalog.admin());
    }
    throw new IllegalArgumentException("Unsupported community type: " + entity.getClass().getSimpleName());
  }

  @ObjectFactory
  protected CommunityTBL createCommunityEntity(Community domain) {
    if (domain == null) {
      return null;
    }
    String name = readField(domain, "name", String.class);
    String description = readField(domain, "description", String.class);
    if (domain instanceof Personal) {
      return PersonalTBL.create(name, description);
    }
    if (domain instanceof Cooperative) {
      return CooperativeTBL.create(name, description);
    }
    if (domain instanceof SystemCommunity) {
      return SystemTBL.create(name, description);
    }
    throw new IllegalArgumentException("Unsupported community type: " + domain.getClass().getSimpleName());
  }

  @AfterMapping
  protected void enrichParticipants(CommunityTBL entity, @MappingTarget Community community) {
    if (!(community instanceof Cooperative organization)) {
      return;
    }
    List<ParticipantTBL> participants = entity.getParticipants();
    if (participants == null || participants.isEmpty()) {
      return;
    }
    UUID adminId = resolveAdminIdValue(participants);
    CommunityPolicy policy = c -> true;
    for (ParticipantTBL participant : participants) {
      if (adminId != null && adminId.equals(participant.getUserId())) {
        continue;
      }
      organization.addMember(
        Participant.reconstitute(
            participant.getId(),
            participant.getUserId(),
            mapRole(participant.getRole()),
            resolveEngagementState(participant)
        ),
        policy
      );
    }
  }

  @AfterMapping
  protected void enrichDomain(CommunityTBL entity, @MappingTarget Community community) {
    if (entity == null || community == null) {
      return;
    }
    writeField(community, "id", CommunityId.of(entity.getId()));
  }

  @AfterMapping
  protected void enrichEntity(Community domain, @MappingTarget CommunityTBL entity) {
    if (domain == null || entity == null) {
      return;
    }
    writeField(entity, "id", domain.getId().getValue());
    Set<Participant> participants = readField(domain, "participants", Set.class);
    if (participants == null || participants.isEmpty()) {
      return;
    }
    for (Participant participant : participants) {
      if (participant == null) {
        continue;
      }
      RoleDescriptor role = participant.getRole();
      ParticipantTBL tbl = ParticipantTBL.create(
          entity,
          participant.getUserId(),
          role == null ? null : role.getName(),
          participant.getEngagementState()
      );
      entity.getParticipants().add(tbl);
    }
  }

  private static UUID resolveAdminId(List<ParticipantTBL> participants) {
    UUID adminId = resolveAdminIdValue(participants);
    if (adminId != null) {
      return adminId;
    }
    if (participants == null || participants.isEmpty()) {
      return UUID.randomUUID();
    }
    return participants.get(0).getUserId();
  }

  private static UUID resolveAdminIdValue(List<ParticipantTBL> participants) {
    if (participants == null) {
      return null;
    }
    return participants.stream()
      .filter(participant -> "ADMIN".equalsIgnoreCase(participant.getRole()))
      .findFirst()
      .map(ParticipantTBL::getUserId)
      .orElse(null);
  }

  private static EngagementState resolveEngagementState(ParticipantTBL participant) {
    if (participant == null || participant.getEngagementState() == null) {
      return EngagementState.ACTIVE;
    }
    return EngagementState.valueOf(participant.getEngagementState());
  }

  private static <T> T readField(Object source, String fieldName, Class<T> type) {
    try {
      Field field = source.getClass().getSuperclass().getDeclaredField(fieldName);
      field.setAccessible(true);
      Object value = field.get(source);
      return type.cast(value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to read field: " + fieldName, e);
    }
  }

  private static void writeField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to write field: " + fieldName, e);
    }
  }
}
