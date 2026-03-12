package com.payvue.app.application.alarm;

import com.payvue.app.infrastructure.persistence.alarm.AlarmReadEntity;
import com.payvue.app.infrastructure.persistence.alarm.AlarmReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmReadService {

  private final AlarmReadJpaRepository alarmReadJpaRepository;

  public void markAsRead(String alarmId, String reader) {
    if (alarmId == null || alarmId.isBlank()) {
      throw new IllegalArgumentException("alarmId는 필수입니다.");
    }
    if (reader == null || reader.isBlank()) {
      throw new IllegalArgumentException("reader는 필수입니다.");
    }
    if (!alarmReadJpaRepository.existsByAlarmIdAndReader(alarmId, reader)) {
      alarmReadJpaRepository.save(new AlarmReadEntity(alarmId, reader));
    }
  }
}
