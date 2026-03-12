package com.payvue.supporting.interaction;

import com.payvue.supporting.interaction.ApprovalDecision;
import com.payvue.supporting.interaction.Intent;
import com.payvue.supporting.interaction.RequiresApproval;
import lombok.Getter;


@Getter
public final class RejectDecision<I extends Intent & RequiresApproval> extends ApprovalDecision<I> {

    private final String reason;
    
    public RejectDecision(I intent, String reason) {
        super(intent);
        this.reason = reason;
    }
}
