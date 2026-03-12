package com.payvue.app.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.core.community.Community;
import com.payvue.core.community.RoleDescriptor;
import java.lang.reflect.Field;
import java.util.UUID;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CommunityEntityMapperTest.TestConfig.class)
class CommunityEntityMapperTest {

  @Configuration
  static class TestConfig {

    @Bean
    RoleCatalog roleCatalog() {
      return RoleCatalog.builder()
          .register(Role.ADMIN)
          .register(Role.MEMBER)
          .register(Role.GUEST)
          .build();
    }

    @Bean
    CommunityEntityMapper communityEntityMapper() {
      return new CommunityEntityMapperImpl();
    }
  }

  @Resource
  private CommunityEntityMapper mapper;

  @Resource
  private RoleCatalog roleCatalog;

  @Test
  @DisplayName("Cooperative 도메인을 엔티티로 변환하면서 참가자를 보존한다")
  void mapsCooperativeToEntity() {
    UUID adminId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID memberId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    RoleDescriptor adminRole = roleCatalog.admin();
    RoleDescriptor memberRole = roleCatalog.member();

    Cooperative organization = Cooperative.create("Org", "desc", adminId, adminRole, memberRole);
    organization.addMember(memberId, community -> true);

    CommunityTBL entity = mapper.toEntity(organization);

    assertThat(entity).isInstanceOf(CooperativeTBL.class);
    assertThat(entity.getId()).isEqualTo(organization.getId().getValue());
    assertThat(entity.getParticipants()).hasSize(2);
    assertThat(entity.getParticipants().stream().map(ParticipantTBL::getRole))
        .contains("ADMIN", "MEMBER");
  }

  @Test
  @DisplayName("Cooperative 엔티티를 도메인으로 변환하면서 역할을 복원한다")
  void mapsCooperativeToDomain() {
    UUID communityId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    UUID adminId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    UUID memberId = UUID.fromString("55555555-5555-5555-5555-555555555555");

    CooperativeTBL entity = CooperativeTBL.create("Org", "desc");
    setField(entity, "id", communityId);

    ParticipantTBL admin = ParticipantTBL.create(entity, adminId, "ADMIN");
    ParticipantTBL member = ParticipantTBL.create(entity, memberId, "MEMBER");
    entity.getParticipants().add(admin);
    entity.getParticipants().add(member);

    Community community = mapper.toDomain(entity);

    assertThat(community.getId().getValue()).isEqualTo(communityId);
    assertThat(community.getParticipantCount()).isEqualTo(2);
    assertThat(community.isAdmin(adminId)).isTrue();
    assertThat(community.getParticipantRole(memberId).getName()).isEqualTo("MEMBER");
  }

  @Test
  @DisplayName("Personal 엔티티는 관리자 한 명만 도메인에 반영한다")
  void mapsPersonalToDomainWithSingleParticipant() {
    UUID communityId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    UUID adminId = UUID.fromString("77777777-7777-7777-7777-777777777777");

    PersonalTBL entity = PersonalTBL.create("Personal", "desc");
    setField(entity, "id", communityId);

    ParticipantTBL admin = ParticipantTBL.create(entity, adminId, "ADMIN");
    entity.getParticipants().add(admin);

    Community community = mapper.toDomain(entity);

    assertThat(community.getId().getValue()).isEqualTo(communityId);
    assertThat(community.getParticipantCount()).isEqualTo(1);
    assertThat(community.isAdmin(adminId)).isTrue();
  }

  private static void setField(Object target, String name, Object value) {
    try {
      Field field = target.getClass().getSuperclass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set field: " + name, e);
    }
  }
}
