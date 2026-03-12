package com.payvue.app.infrastructure.persistence.community;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CommunityJpaRepositoryTest {

  @Autowired
  private CommunityJpaRepository repository;

  @Test
  @DisplayName("커뮤니티 엔티티를 업데이트한다")
  void updatesCommunity() {
    UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
    CooperativeTBL entity = CooperativeTBL.create("Org", "desc");
    setField(entity, "id", id);
    repository.save(entity);

    CooperativeTBL updated = CooperativeTBL.create("New Org", "new desc");
    setField(updated, "id", id);

    int count = repository.updateCommunity(updated);

    assertThat(count).isEqualTo(1);
    CommunityTBL found = repository.findById(id).orElseThrow();
    assertThat(found.getName()).isEqualTo("New Org");
    assertThat(found.getDescription()).isEqualTo("new desc");
  }

  @Test
  @DisplayName("참여자와 함께 커뮤니티를 조회한다")
  void findsByIdsWithParticipants() {
    UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    CooperativeTBL entity = CooperativeTBL.create("Org", "desc");
    setField(entity, "id", id);
    ParticipantTBL admin = ParticipantTBL.create(entity, UUID.fromString("12121212-1212-1212-1212-121212121212"), "ADMIN");
    entity.getParticipants().add(admin);
    repository.save(entity);

    List<CommunityTBL> results = repository.findAllByIdInWithParticipants(Set.of(id));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getParticipants()).hasSize(1);
  }

  @Test
  @DisplayName("ID 목록이 비어 있으면 빈 결과를 반환한다")
  void returnsEmptyWhenIdsEmpty() {
    List<CommunityTBL> results = repository.findAllByIdInWithParticipants(Set.of());

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("이름으로 존재 여부를 확인한다")
  void existsByName() {
    UUID id = UUID.fromString("99999999-8888-7777-6666-555555555555");
    PersonalTBL entity = PersonalTBL.create("Personal", "desc");
    setField(entity, "id", id);
    repository.save(entity);

    assertThat(repository.existsByName("Personal")).isTrue();
    assertThat(repository.existsByName("Unknown")).isFalse();
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
