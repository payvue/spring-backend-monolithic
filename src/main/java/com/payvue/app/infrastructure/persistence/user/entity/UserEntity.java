package com.payvue.app.infrastructure.persistence.user.entity;

import com.payvue.supporting.user.command.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "last_logged_in")
    private Instant lastLoggedIn;

    @Column(name = "is_logged_in", nullable = false)
    private boolean loggedIn;

    @Column(name = "display_name", nullable = false, length = 30)
    private String displayName;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "sido_code", length = 20)
    private String sidoCode;

    @Column(name = "sigungu_code", length = 20)
    private String sigunguCode;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserMembershipEntity> memberships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPasswordHistoryEntity> passwordHistories = new ArrayList<>();

    public void addPasswordHistory(UserPasswordHistoryEntity history) {
        if (history == null) {
            return;
        }
        history.setUser(this);
        passwordHistories.add(history);
    }

    public void clearPasswordHistories() {
        passwordHistories.clear();
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<UserFeaturePreferenceEntity> featurePreferences = new ArrayList<>();
}
