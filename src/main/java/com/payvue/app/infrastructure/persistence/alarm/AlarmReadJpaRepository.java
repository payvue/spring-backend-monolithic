package com.payvue.app.infrastructure.persistence.alarm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmReadJpaRepository extends JpaRepository<AlarmReadEntity, Long> {
  boolean existsByAlarmIdAndReader(String alarmId, String reader);
  List<AlarmReadEntity> findByAlarmIdInAndReader(List<String> alarmIds, String reader);
}
