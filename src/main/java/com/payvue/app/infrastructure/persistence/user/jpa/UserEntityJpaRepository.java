package com.payvue.app.infrastructure.persistence.user.jpa;

import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserEntityJpaRepository extends JpaRepository<UserEntity, String> {
  Optional<UserEntity> findByLoginId(String loginId);

  Optional<UserEntity> findByEmail(String email);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update UserEntity u
         set u.loginId = :#{#entity.loginId},
             u.email = :#{#entity.email},
             u.phoneNumber = :#{#entity.phoneNumber},
             u.emailVerified = :#{#entity.emailVerified},
             u.phoneVerified = :#{#entity.phoneVerified},
             u.lastLoggedIn = :#{#entity.lastLoggedIn},
             u.loggedIn = :#{#entity.loggedIn},
             u.displayName = :#{#entity.displayName},
             u.realName = :#{#entity.realName},
             u.dateOfBirth = :#{#entity.dateOfBirth},
             u.profileImageUrl = :#{#entity.profileImageUrl},
             u.bio = :#{#entity.bio},
             u.status = :#{#entity.status},
             u.password = :#{#entity.password},
             u.sidoCode = :#{#entity.sidoCode},
             u.sigunguCode = :#{#entity.sigunguCode},
             u.roadAddress = :#{#entity.roadAddress},
             u.detailAddress = :#{#entity.detailAddress},
             u.postalCode = :#{#entity.postalCode},
             u.updatedAt = :#{#entity.updatedAt}
       where u.id = :#{#entity.id}
      """)
  int updateUser(@Param("entity") UserEntity user);
}
