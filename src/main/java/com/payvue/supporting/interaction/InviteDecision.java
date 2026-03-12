package com.payvue.supporting.interaction;

import lombok.Getter;

/**
 * InviteDecision - 초대 결정
 */
@Getter
public final class InviteDecision extends ActionDecision<ManageIntent> implements RequiresContext {
    
    public InviteDecision(ManageIntent intent) {
        super(intent);
    }
}
