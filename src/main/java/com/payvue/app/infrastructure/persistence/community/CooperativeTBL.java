package com.payvue.app.infrastructure.persistence.community;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
/**
 * CooperativeTBL - Cooperative Community의 JPA Entity
 */
@Entity
@DiscriminatorValue("COOPERATIVE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CooperativeTBL extends CommunityTBL {

    public CooperativeTBL(String name, String description) {
        super(name, description);
    }

    public static CooperativeTBL create(String name, String description) {
        return new CooperativeTBL(name, description);
    }
}
