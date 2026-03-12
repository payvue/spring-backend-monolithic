package com.payvue.app.infrastructure.persistence.user.jpa;

import com.payvue.app.infrastructure.persistence.user.entity.UserKycEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserKycJpaRepository extends JpaRepository<UserKycEntity, String> {
  Optional<UserKycEntity> findByUserId(String userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update UserKycEntity k
         set k.residentRegistrationNo = :#{#entity.residentRegistrationNo},
             k.status = :#{#entity.status},
             k.verifiedAt = :#{#entity.verifiedAt}
       where k.userId = :#{#entity.userId}
      """)
  int updateKyc(@Param("entity") UserKycEntity entity);
}
