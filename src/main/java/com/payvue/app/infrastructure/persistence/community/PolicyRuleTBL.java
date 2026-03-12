package com.payvue.app.infrastructure.persistence.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

/**
 * PolicyRuleTBL - Policy Rule JPA Entity
 * 
 * CommunityTBL과 1:N 관계
 * rule_type과 JSON config로 다양한 Rule 저장
 */
@Entity
@Table(name = "policy_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyRuleTBL {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private CommunityTBL community;

    @Column(name = "rule_type", nullable = false, length = 100)
    private String ruleType;

    @Column(name = "rule_config", columnDefinition = "TEXT")
    private String ruleConfig;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    public PolicyRuleTBL(CommunityTBL community, String ruleType, String ruleConfig, Integer orderIndex) {
        this.community = community;
        this.ruleType = ruleType;
        this.ruleConfig = ruleConfig;
        this.orderIndex = orderIndex;
    }

    public static PolicyRuleTBL create(CommunityTBL community, String ruleType, String ruleConfig, Integer orderIndex) {
        return new PolicyRuleTBL(community, ruleType, ruleConfig, orderIndex);
    }

    void setCommunity(CommunityTBL community) {
        this.community = community;
    }
}
