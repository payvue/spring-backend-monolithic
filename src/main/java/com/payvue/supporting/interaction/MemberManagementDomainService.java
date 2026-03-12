package com.payvue.supporting.interaction;

import com.payvue.core.community.Participant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
class MemberManagementDomainService implements DomainService<ManageIntent> {
    
    private static DomainService instance = new MemberManagementDomainService();

    public static DomainService of() {
        if(instance == null)
            instance = new MemberManagementDomainService();
        return instance;
    }

    @Override
    public boolean supports(Decision<?> decision) {
        return decision.getIntent().getCategory() == IntentCategory.MANAGE.name();
    }

    @Override
    public Interaction execute(InteractionContext context, Decision<ManageIntent> decision) {
        throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
    }

    @Override
    public Interaction execute(Decision<ManageIntent> decision) {
        throw new UnsupportedOperationException("컨텍스트 기반 실행이 필요합니다.");
    }

    @Override
    public Interaction execute(
        InteractionContext context,
        Decision<ManageIntent> decision,
        InteractionHandler<ManageIntent> handler
    ) {
        Interaction interaction = Interaction.pending(decision);
        if (!(context instanceof InternalInteractionContext internal)) {
            throw new IllegalArgumentException();
        }
        Participant initiator = decision.getIntent().initiator();
        if (internal.getCommunity().isParticipant(initiator.getUserId())) {
            if (!decision.getIntent().participant().equals(initiator)) {
                handler.handle(context, decision);
                return interaction;
            }
        }
        throw new IllegalArgumentException();
    }
}
