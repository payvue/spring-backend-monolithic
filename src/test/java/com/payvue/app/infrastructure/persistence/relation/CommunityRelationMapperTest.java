package com.payvue.app.infrastructure.persistence.relation;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.app.domain.community.Personal;
import com.payvue.app.domain.community.Role;
import com.payvue.app.domain.community.RoleCatalog;
import com.payvue.core.community.Community;
import com.payvue.core.relation.CommunityRelation;
import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityRelationMapperTest {

  private final CommunityRelationMapper mapper = new CommunityRelationMapper() {};

  @Test
  @DisplayName("도메인 관계를 엔티티로 변환한다")
  void mapsRelationToEntity() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    UUID fromAdmin = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID toAdmin = UUID.fromString("22222222-2222-2222-2222-222222222222");

    Community from = Personal.create("From", "desc", fromAdmin, roleCatalog.admin());
    Community to = Personal.create("To", "desc", toAdmin, roleCatalog.admin());

    CommunityRelation relation = CommunityRelation.associateWith(from, to);

    CommunityRelationTBL entity = mapper.toEntity(relation);

    assertThat(entity.getFromCommunityId()).isEqualTo(from.getId().getValue());
    assertThat(entity.getToCommunityId()).isEqualTo(to.getId().getValue());
    assertThat(entity.getRelationType()).isEqualTo(RelationType.FRENDSHIP);
    assertThat(entity.getStatus()).isEqualTo(relation.getStatus());
  }

  @Test
  @DisplayName("엔티티 관계를 도메인으로 변환한다")
  void mapsRelationToDomain() {
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    UUID fromAdmin = UUID.fromString("33333333-3333-3333-3333-333333333333");
    UUID toAdmin = UUID.fromString("44444444-4444-4444-4444-444444444444");

    Community from = Personal.create("From", "desc", fromAdmin, roleCatalog.admin());
    Community to = Personal.create("To", "desc", toAdmin, roleCatalog.admin());

    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        from.getId().getValue(),
        to.getId().getValue(),
        RelationType.FRENDSHIP,
        ConnectionStatus.PENDING
    );

    CommunityRelation relation = mapper.toDomain(entity, from, to);

    assertThat(relation.getFrom()).isEqualTo(from);
    assertThat(relation.getTo()).isEqualTo(to);
    assertThat(relation.getRelationType()).isEqualTo(RelationType.FRENDSHIP);
    assertThat(relation.getStatus()).isEqualTo(ConnectionStatus.PENDING);
  }

  @Test
  @DisplayName("상태 변경을 엔티티에 반영한다")
  void updatesEntityStatus() {
    CommunityRelationTBL entity = CommunityRelationTBL.create(
        UUID.fromString("66666666-6666-6666-6666-666666666666"),
        UUID.fromString("77777777-7777-7777-7777-777777777777"),
        UUID.fromString("88888888-8888-8888-8888-888888888888"),
        RelationType.FRENDSHIP,
        ConnectionStatus.PENDING
    );
    RoleCatalog roleCatalog = RoleCatalog.builder()
        .register(Role.ADMIN)
        .register(Role.MEMBER)
        .register(Role.GUEST)
        .build();
    Community from = Personal.create("From", "desc", UUID.randomUUID(), roleCatalog.admin());
    Community to = Personal.create("To", "desc", UUID.randomUUID(), roleCatalog.admin());
    CommunityRelation relation = CommunityRelation.reconstitute(
        com.payvue.core.relation.RelationId.of(entity.getId()),
        from,
        to,
        entity.getRelationType(),
        ConnectionStatus.ACTIVE
    );

    mapper.updateEntity(relation, entity);

    assertThat(entity.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
  }
}
