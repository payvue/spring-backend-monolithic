package com.payvue.app.infrastructure.persistence.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import com.payvue.common.status.EngagementState;

/**
 * ParticipantTBL - Community 참여자 JPA Entity
 * 
 * Community Aggregate 내부 Entity
 * CommunityTBL과 1:N 관계
 */
@Entity
@Table(name = "participant", uniqueConstraints = {
    @UniqueConstraint(name = "uk_participant_community_user", columnNames = {"community_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantTBL {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private CommunityTBL community;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "engagement_state", nullable = false, length = 20)
    private String engagementState;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public ParticipantTBL(CommunityTBL community, UUID userId, String role, EngagementState engagementState) {
        this.community = community;
        this.userId = userId;
        this.role = role;
        this.engagementState = engagementState == null ? EngagementState.PENDING.name() : engagementState.name();
        this.joinedAt = Instant.now();
    }

    public static ParticipantTBL create(CommunityTBL community, UUID userId, String role) {
        return new ParticipantTBL(community, userId, role, EngagementState.PENDING);
    }

    public static ParticipantTBL create(
        CommunityTBL community,
        UUID userId,
        String role,
        EngagementState engagementState
    ) {
        return new ParticipantTBL(community, userId, role, engagementState);
    }

    void setCommunity(CommunityTBL community) {
        this.community = community;
    }
}
