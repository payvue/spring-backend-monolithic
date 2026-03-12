package com.payvue.app.infrastructure.persistence.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlarmJpaRepository extends JpaRepository<AlarmEntity, Long> {
    Optional<AlarmEntity> findByAlarmId(String alarmId);
    Page<AlarmEntity> findByRecipientTypeAndRecipientValueAndAlarmTypeOrderByCreatedAtDesc(
            String recipientType,
            String recipientValue,
            String alarmType,
            Pageable pageable
    );

    @Query(
        value = """
            select a
            from AlarmEntity a
            left join AlarmReadEntity r
              on r.alarmId = a.alarmId
             and r.reader = :reader
            where a.recipientType = :recipientType
              and a.recipientValue in :recipientValues
              and a.alarmType in :alarmTypes
            order by case when r.id is null then 0 else 1 end asc,
                     a.createdAt desc
            """,
        countQuery = """
            select count(a)
            from AlarmEntity a
            where a.recipientType = :recipientType
              and a.recipientValue in :recipientValues
              and a.alarmType in :alarmTypes
            """
    )
    Page<AlarmEntity> findReceivedOrderedForUser(
        @Param("recipientType") String recipientType,
        @Param("recipientValues") java.util.List<String> recipientValues,
        @Param("alarmTypes") java.util.List<String> alarmTypes,
        @Param("reader") String reader,
        Pageable pageable
    );

    @Query(
        value = """
            select a
            from AlarmEntity a
            left join AlarmReadEntity r
              on r.alarmId = a.alarmId
             and r.reader = :reader
            where a.alarmType in :alarmTypes
              and (
                (a.recipientType = :userRecipientType and a.recipientValue in :recipientValues)
                or
                (a.recipientType = :communityRecipientType and a.recipientValue in :communityIds)
              )
            order by case when r.id is null then 0 else 1 end asc,
                     a.createdAt desc
            """,
        countQuery = """
            select count(a)
            from AlarmEntity a
            where a.alarmType in :alarmTypes
              and (
                (a.recipientType = :userRecipientType and a.recipientValue in :recipientValues)
                or
                (a.recipientType = :communityRecipientType and a.recipientValue in :communityIds)
              )
            """
    )
    Page<AlarmEntity> findReceivedOrderedForUserAndCommunities(
        @Param("userRecipientType") String userRecipientType,
        @Param("recipientValues") java.util.List<String> recipientValues,
        @Param("communityRecipientType") String communityRecipientType,
        @Param("communityIds") java.util.List<String> communityIds,
        @Param("alarmTypes") java.util.List<String> alarmTypes,
        @Param("reader") String reader,
        Pageable pageable
    );
}
