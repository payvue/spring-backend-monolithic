package com.payvue.app.infrastructure.persistence.alarm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "alarm_reads",
    uniqueConstraints = @UniqueConstraint(columnNames = {"alarmId", "reader"}),
    indexes = {
        @Index(name = "idx_alarm_read_alarm_id", columnList = "alarmId"),
        @Index(name = "idx_alarm_read_reader", columnList = "reader")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlarmReadEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String alarmId;

  @Column(nullable = false, length = 255)
  private String reader;

  @Column(nullable = false)
  private Instant readAt;

  public AlarmReadEntity(String alarmId, String reader) {
    this.alarmId = alarmId;
    this.reader = reader;
    this.readAt = Instant.now();
  }
}
