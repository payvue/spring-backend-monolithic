package com.payvue.app.domain.auth;

import java.util.Objects;

public class SequenceByNextStep implements NextStep {
    static final String SEQUENCE_META_KEY = "authSequence";

    private final CompletionStrategyClassification.Sequence sequence;
    private final NextStep onMatch;
    private final NextStep onMismatch;

    public SequenceByNextStep(
        CompletionStrategyClassification.Sequence sequence,
        NextStep onMatch
    ) {
        this(sequence, onMatch, new AuthenticateComplete());
    }

    public SequenceByNextStep(
        CompletionStrategyClassification.Sequence sequence,
        NextStep onMatch,
        NextStep onMismatch
    ) {
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.onMatch = Objects.requireNonNull(onMatch, "onMatch");
        this.onMismatch = Objects.requireNonNull(onMismatch, "onMismatch");
    }

    @Override
    public AuthCompletion apply(AuthResult result, AuthContext context, CompletionStrategy delegate) {
        if (result == null || result.isFailure()) {
            return delegate.complete(context, result);
        }

        Object meta = result.meta().get(SEQUENCE_META_KEY);
        if (meta instanceof CompletionStrategyClassification.Sequence seq && seq == sequence) {
            return onMatch.apply(result, context, delegate);
        }

        return onMismatch.apply(result, context, delegate);
    }
}
