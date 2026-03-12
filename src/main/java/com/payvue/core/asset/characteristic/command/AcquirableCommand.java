package com.payvue.core.asset.characteristic.command;

import com.payvue.characteristic.Acquirable;
import com.payvue.core.asset.characteristic.CapabilityCommand;

public interface AcquirableCommand extends CapabilityCommand {
  void apply(Acquirable acquirable);

  final class Purchased implements AcquirableCommand {
    @Override
    public void apply(Acquirable acquirable) {
      acquirable.setMethod(Acquirable.AcquisitionMethod.PURCHASED);
    }
  }

  final class Received implements AcquirableCommand {
    @Override
    public void apply(Acquirable acquirable) {
      acquirable.setMethod(Acquirable.AcquisitionMethod.RECEIVED);
    }
  }

  final class Shared implements AcquirableCommand {
    @Override
    public void apply(Acquirable acquirable) {
      acquirable.setMethod(Acquirable.AcquisitionMethod.SHARED);
    }
  }

  final class Contributed implements AcquirableCommand {
    @Override
    public void apply(Acquirable acquirable) {
      acquirable.setMethod(Acquirable.AcquisitionMethod.CONTRIBUTED);
    }
  }
}
