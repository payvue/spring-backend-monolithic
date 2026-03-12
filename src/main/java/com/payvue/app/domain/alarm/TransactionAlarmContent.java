package com.payvue.app.domain.alarm;

import com.payvue.generic.alarm.AlarmContent;
import com.payvue.generic.alarm.AlarmOperationUnit;
import com.payvue.generic.alarm.Trigger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class TransactionAlarmContent implements AlarmContent {
    private final AlarmOperationUnit operationUnit;
    private final String transactionId;
    private final Long amount;
    private final String merchant;
    private final Trigger trigger;
    private final String approvedBy;
    private final String initiatedBy;
    private final String requestedBy;
    private final String method;

    private TransactionAlarmContent(
            AlarmOperationUnit operationUnit,
            String transactionId,
            Long amount,
            String merchant,
            Trigger trigger,
            String approvedBy,
            String initiatedBy,
            String requestedBy,
            String method
    ) {
        this.operationUnit = Objects.requireNonNull(operationUnit, "operationUnit is required");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.merchant = merchant;
        this.trigger = trigger;
        this.approvedBy = approvedBy;
        this.initiatedBy = initiatedBy;
        this.requestedBy = requestedBy;
        this.method = method;
    }

    @Override
    public String operationKey() {
        return operationUnit.key();
    }

    /**
     * Builder for flexible construction
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AlarmOperationUnit operationUnit;
        private String transactionId;
        private Long amount;
        private String merchant;
        private Trigger trigger;
        private String approvedBy;
        private String initiatedBy;
        private String requestedBy;
        private String method;

        public Builder operationUnit(AlarmOperationUnit operationUnit) {
            this.operationUnit = operationUnit;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder merchant(String merchant) {
            this.merchant = merchant;
            return this;
        }

        public Builder trigger(Trigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder approvedBy(String approvedBy) {
            this.approvedBy = approvedBy;
            return this;
        }

        public Builder initiatedBy(String initiatedBy) {
            this.initiatedBy = initiatedBy;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public TransactionAlarmContent build() {
            return new TransactionAlarmContent(
                    operationUnit,
                    transactionId,
                    amount,
                    merchant,
                    trigger,
                    approvedBy,
                    initiatedBy,
                    requestedBy,
                    method
            );
        }
    }
}
