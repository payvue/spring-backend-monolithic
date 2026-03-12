package com.payvue.supporting.interaction;

import com.payvue.core.asset.AssetItem;
import com.payvue.app.domain.asset.specification.NamingSpecification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
class TransferDomainService implements DomainService<TransferAssetIntent> {
    
    private static DomainService instance = new TransferDomainService();
    
    public static DomainService of() {
        if(instance == null)
            instance = new TransferDomainService();
        return instance;
    }

    @Override
    public boolean supports(Decision<?> decision) {
        return decision.getIntent().getCategory() == com.payvue.supporting.interaction.IntentCategory.TRANSFER.name();
    }

    @Override
    public Interaction execute(Decision<TransferAssetIntent> decision) {
        throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
    }

    @Override
    public Interaction execute(
        InteractionContext context,
        Decision<TransferAssetIntent> decision,
        InteractionHandler<TransferAssetIntent> handler
    ) {
        Interaction interaction = Interaction.pending(decision);

        AssetItem item = decision.getIntent().item();
        Set<AssetItem> holds = decision.getIntent().from().ownedAssetsConformingTo(new NamingSpecification(item.getName()));

        if(holds.isEmpty()) {
            throw new IllegalArgumentException();
        }

        handler.handle(context, decision);

        return interaction;
    }
}
