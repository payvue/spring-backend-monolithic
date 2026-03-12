package com.payvue.app.infrastructure.persistence.alarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.generic.alarm.Alarm;
import com.payvue.generic.alarm.AlarmId;
import com.payvue.generic.alarm.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
@RequiredArgsConstructor
class JpaAlarmRepository implements AlarmRepository {
    
    private final AlarmJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Alarm save(Alarm alarm) {
        try {
            AlarmEntity entity = AlarmEntity.toEntity(alarm, objectMapper);
            jpaRepository.findByAlarmId(entity.getAlarmId())
                    .ifPresent(existing -> entity.applyId(existing.getId()));
            jpaRepository.save(entity);

            return alarm;

        } catch (Exception e) {
            throw new RuntimeException("Alarm 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<Alarm> findById(AlarmId alarmId) {
        return jpaRepository.findByAlarmId(alarmId.value().toString())
                .map(entity -> AlarmEntity.toDomain(entity, objectMapper));
    }
}
