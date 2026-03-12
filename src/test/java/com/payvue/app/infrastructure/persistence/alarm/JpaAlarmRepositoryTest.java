package com.payvue.app.infrastructure.persistence.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAlarmRepositoryTest.TestConfig.class)
class JpaAlarmRepositoryTest {

  @TestConfiguration
  static class TestConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    JpaAlarmRepository jpaAlarmRepository(
        AlarmJpaRepository alarmJpaRepository,
        ObjectMapper objectMapper
    ) {
      return new JpaAlarmRepository(alarmJpaRepository, objectMapper);
    }
  }

  @Autowired
  private JpaAlarmRepository repository;

  @Test
  @DisplayName("알람을 저장하고 조회한다")
  void savesAndFindsAlarm() {
    Alarm alarm = AlarmFactory.createVerificationEmail(
        "user@payvue.com",
        "PayVue",
        "verify",
        "https://payvue.com/verify"
    );

    repository.save(alarm);

    assertThat(repository.findById(alarm.getId())).isPresent();
  }

  @Test
  @DisplayName("없는 알람은 조회되지 않는다")
  void returnsEmptyWhenMissing() {
    assertThat(repository.findById(new com.payvue.generic.alarm.AlarmId(java.util.UUID.randomUUID()))).isEmpty();
  }
}
