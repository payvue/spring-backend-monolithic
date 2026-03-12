package com.payvue.supporting.interaction;

import com.payvue.core.community.RoleDescriptor;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class ChangeRoleDecision extends ActionDecision<ManageIntent> implements RequiresContext {
    
    private final RoleDescriptor newRole;
    
    public ChangeRoleDecision(ManageIntent intent, RoleDescriptor newRole) {
        super(intent);
        this.newRole = Objects.requireNonNull(newRole, "newRole은 필수입니다");
    }
}
