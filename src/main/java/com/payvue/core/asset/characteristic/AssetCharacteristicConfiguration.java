package com.payvue.core.asset.characteristic;

import com.payvue.characteristic.Acquirable;
import com.payvue.characteristic.Consumable;
import com.payvue.characteristic.HasBalance;
import com.payvue.core.asset.characteristic.command.AcquirableCommand;
import com.payvue.core.asset.characteristic.command.QuantityCommand;
import com.payvue.core.asset.characteristic.command.DepositCommand;
import com.payvue.core.asset.characteristic.command.PayableCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetCharacteristicConfiguration {

  @Bean
  public CapabilityManager capabilityManager() {
    CapabilityExecutionContext context = CapabilityExecutionContext.builder()
        .register(HasBalance.class, PayableCommand.class, new BalancePayHandler(new PayablePolicy()))
        .register(HasBalance.class, DepositCommand.class, new BalanceDepositHandler( new DepositPolicy()))
        .register(
            Consumable.class,
            QuantityCommand.Consume.class,
            new ConsumeHandler(new ConsumableConsumePolicy())
        )
        .register(
                Consumable.class,
                QuantityCommand.Produce.class,
                new ProduceHandler()
        )
        .register(Acquirable.class, AcquirableCommand.class, new AcquirableCommandHandler(new AcquirablePolicy()))
        .build();
    return new DefaultCapabilityManager(context);
  }
}
