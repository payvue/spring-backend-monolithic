package com.payvue.app.infrastructure.persistence.community;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SystemTBL - System Community의 JPA Entity
 */
@Entity
@DiscriminatorValue("SYSTEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemTBL extends CommunityTBL {

    public SystemTBL(String name, String description) {
        super(name, description);
    }

    public static SystemTBL create(String name, String description) {
        return new SystemTBL(name, description);
    }
}
