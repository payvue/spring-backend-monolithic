package com.payvue.supporting.interaction;

public final class ApproveDecision<I extends Intent & RequiresApproval> extends ApprovalDecision<I> {
    
    public ApproveDecision(I intent) {
        super(intent);
    }
}
