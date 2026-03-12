package com.payvue.app.infrastructure.persistence.community;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
/**
 * PersonalTBL - Personal Community의 JPA Entity
 */
@Entity
@DiscriminatorValue("PERSONAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalTBL extends CommunityTBL {

    public PersonalTBL(String name, String description) {
        super(name, description);
    }

    public static PersonalTBL create(String name, String description) {
        return new PersonalTBL(name, description);
    }
}
