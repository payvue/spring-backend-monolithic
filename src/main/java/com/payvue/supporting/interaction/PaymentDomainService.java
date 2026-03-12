package com.payvue.supporting.interaction;

import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.AssetClassification;
import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.category.Cash;
import com.payvue.core.hold.HoldingCapability;
import com.payvue.app.domain.asset.specification.CategorySpecification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class PaymentDomainService implements DomainService<ExecutePaymentIntent> {
    
    private static DomainService<ExecutePaymentIntent> instance = new PaymentDomainService();
    
    public static DomainService<ExecutePaymentIntent> of() {
        if(instance == null)
            instance = new PaymentDomainService();
        return instance;
    }

    @Override
    public boolean supports(Decision<?> decision) {
        return decision.getIntent().getCategory() == IntentCategory.PAYMENT.name();
    }

    @Override
    public Interaction execute(Decision<ExecutePaymentIntent> decision) {
        throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
    }

    @Override
    public Interaction execute(
        InteractionContext context,
        Decision<ExecutePaymentIntent> decision,
        InteractionHandler<ExecutePaymentIntent> handler
    ) {
        Interaction interaction = Interaction.pending(decision);

        HoldingCapability holdings = decision.getIntent().holdings();
        AssetItem item = decision.getIntent().item();

        Cash cash = holdings
                .ownedSingleAssetsConformingTo(
                        new CategorySpecification(AssetClassification.cash())
                )
                .filter(Cash.class::isInstance)
                .map(Cash.class::cast)
                .orElseThrow(IllegalArgumentException::new);

        return item.getCapability(HasBalance.class)
                .filter(payable ->
                        cash.credit().isGreaterThan(payable.balance())
                )
                .map(ignored -> {
                    try {
                        handler.handle(context, decision);
                    } catch (IllegalArgumentException e) {
                        interaction.reject(e.getMessage());
                    }
                    return interaction;
                })
                .orElseThrow(IllegalArgumentException::new);
    }
}
