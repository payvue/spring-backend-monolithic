package com.payvue.supporting.interaction;

import com.payvue.core.asset.characteristic.AssetCharacteristicConfiguration;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.app.domain.paymentrequest.PaymentRequestRepository;
import com.payvue.core.hold.HoldingsRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AssetCharacteristicConfiguration.class)
public class InteractionConfiguration {

    @Bean
    public InteractionFlowRegistry interactionFlowRegistry(
        CapabilityManager capabilityManager,
        InteractionContextFactory interactionContextFactory,
        ObjectProvider<PaymentRequestRepository> paymentRequestRepositoryProvider,
        ObjectProvider<HoldingsRepository> holdingsRepositoryProvider
    ) {
        InteractionFlowRegistry registry = new InteractionFlowRegistry();
        registry.flow("management")
            .domainService(MemberManagementDomainService.of())
            .handler(
                ChainedEventHandler.chain(new InviteMemberHandler())
                    .add(new ChangeRoleHandler())
                    .firstMatch()
                    .build()
            )
            .build();

        registry.flow("payment")
            .domainService(PaymentDomainService.of())
            .handler(
                ChainedEventHandler.chain(new ApprovePaymentHandler(capabilityManager))
                    .add(new RejectPaymentHandler())
                    .firstMatch()
                    .build()
            )
            .build();

        PaymentRequestRepository paymentRequestRepository = paymentRequestRepositoryProvider.getIfAvailable();
        HoldingsRepository holdingsRepository = holdingsRepositoryProvider.getIfAvailable();
        if (paymentRequestRepository != null && holdingsRepository != null) {
            InteractionExecutor paymentExecutor =
                new InteractionExecutor(interactionContextFactory, registry);
            registry.flow("payment-request")
                .domainService(PaymentRequestDomainService.of(paymentRequestRepository, holdingsRepository))
                .handler(
                    ChainedEventHandler.chain(new CreatePaymentRequestHandler(paymentRequestRepository))
                        .add(new ApprovePaymentRequestHandler(
                            paymentRequestRepository,
                            holdingsRepository,
                            paymentExecutor
                        ))
                        .add(new RejectPaymentRequestHandler(paymentRequestRepository))
                        .firstMatch()
                        .build()
                )
                .build();
        }

        registry.flow("transfer-cash")
            .domainService(TransferCashDomainService.of())
            .handler(
                ChainedEventHandler.chain(new ApproveTransferCashHandler(capabilityManager))
                    .firstMatch()
                    .build()
            )
            .build();

        registry.flow("transfer")
            .domainService(TransferDomainService.of())
            .handler(
                ChainedEventHandler.chain(new ApproveTransferAssetHandler())
                    .firstMatch()
                    .build()
            )
            .build();

        return registry;
    }

    @Bean
    public InteractionContextFactory interactionContextFactory() {
        return new InteractionContextFactory();
    }

    @Bean
    public InteractionExecutor interactionExecutor(
            InteractionContextFactory interactionContextFactory,
            InteractionFlowRegistry interactionFlowRegistry) {
        return new InteractionExecutor(interactionContextFactory, interactionFlowRegistry);
    }
}
