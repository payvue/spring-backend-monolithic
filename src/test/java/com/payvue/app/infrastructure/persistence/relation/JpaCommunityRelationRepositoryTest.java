package com.payvue.app.infrastructure.persistence.relation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Cooperative;
import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.app.infrastructure.persistence.community.CommunityEntityMapper;
import com.payvue.app.infrastructure.persistence.community.CommunityEntityMapperImpl;
import com.payvue.app.infrastructure.persistence.community.CommunityJpaRepository;
import com.payvue.app.infrastructure.persistence.community.CommunityTBL;
import com.payvue.app.infrastructure.persistence.community.CooperativeTBL;
import com.payvue.app.infrastructure.persistence.community.ParticipantTBL;
import com.payvue.app.infrastructure.persistence.community.PersonalTBL;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.relation.CommunityRelation;
import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationId;
import com.payvue.core.relation.RelationType;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaCommunityRelationRepositoryTest.TestConfig.class)
class JpaCommunityRelationRepositoryTest {

  @TestConfiguration
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

    @Bean
    CommunityRelationMapper communityRelationMapper() {
      return new CommunityRelationMapper() {};
    }

    @Bean
    JpaCommunityRelationRepository jpaCommunityRelationRepository(
        CommunityRelationJpaRepository relationJpaRepository,
        CommunityJpaRepository communityJpaRepository,
        CommunityRelationMapper relationMapper,
        CommunityEntityMapper communityEntityMapper
    ) {
      return new JpaCommunityRelationRepository(
          relationJpaRepository,
          communityJpaRepository,
          relationMapper,
          communityEntityMapper
      );
    }
  }

  @Autowired
  private JpaCommunityRelationRepository repository;

  @Autowired
  private CommunityJpaRepository communityJpaRepository;

  @Autowired
  private RoleCatalog roleCatalog;

  @Test
  @DisplayName("관계를 저장하고 조회한다")
  void savesAndFindsRelation() {
    Community from = Personal.create("From", "desc", UUID.randomUUID(), roleCatalog.admin());
    Community to = Cooperative.create("To", "desc", UUID.randomUUID(), roleCatalog.admin(), roleCatalog.member());

    saveCommunityEntity(from);
    saveCommunityEntity(to);

    CommunityRelation relation = CommunityRelation.associateWith(from, to);
    repository.save(relation);

    Optional<CommunityRelation> found = repository.findById(relation.getId());

    assertThat(found).isPresent();
    assertThat(repository.existsRelationBetween(from, to)).isTrue();
  }

  @Test
  @DisplayName("관계 상태 변경을 저장한다")
  void updatesRelationStatus() {
    Community from = Personal.create("From", "desc", UUID.randomUUID(), roleCatalog.admin());
    Community to = Personal.create("To", "desc", UUID.randomUUID(), roleCatalog.admin());

    saveCommunityEntity(from);
    saveCommunityEntity(to);

    CommunityRelation relation = CommunityRelation.associateWith(from, to);
    repository.save(relation);

    relation.activate();
    repository.save(relation);

    CommunityRelation reloaded = repository.findById(relation.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
  }

  @Test
  @DisplayName("커뮤니티 기준으로 관계를 조회한다")
  void findsRelationsByStatus() {
    Community from = Personal.create("From", "desc", UUID.randomUUID(), roleCatalog.admin());
    Community to = Personal.create("To", "desc", UUID.randomUUID(), roleCatalog.admin());

    saveCommunityEntity(from);
    saveCommunityEntity(to);

    CommunityRelation relation = CommunityRelation.reconstitute(
        RelationId.generate(),
        from,
        to,
        RelationType.FRENDSHIP,
        ConnectionStatus.PENDING
    );
    repository.save(relation);

    assertThat(repository.findPendingRelationsOf(from)).hasSize(1);
    assertThat(repository.findActiveRelationsOf(from)).isEmpty();
  }

  private void saveCommunityEntity(Community community) {
    CommunityTBL entity = community instanceof Cooperative
        ? CooperativeTBL.create(community.getName(), "desc")
        : PersonalTBL.create(community.getName(), "desc");
    setField(entity, "id", community.getId().getValue());
    UUID adminId = community.getParticipantIds().stream()
        .filter(id -> community.getParticipantRole(id).isAdmin())
        .findFirst()
        .orElseThrow();
    ParticipantTBL admin = ParticipantTBL.create(entity, adminId, "ADMIN");
    entity.getParticipants().add(admin);
    communityJpaRepository.save(entity);
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
