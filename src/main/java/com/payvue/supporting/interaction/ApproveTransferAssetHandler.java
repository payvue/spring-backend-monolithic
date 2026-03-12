package com.payvue.supporting.interaction;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.characteristic.CapabilityManager;
import com.payvue.core.hold.Holdings;
import com.payvue.app.domain.asset.specification.NamingSpecification;
import com.payvue.core.hold.HoldingsRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ApproveTransferAssetHandler implements InteractionHandler<TransferAssetIntent> {

  @Override
  public boolean supports(Decision<?> decision) {
    return decision instanceof ApproveDecision;
  }

  @Override
  public void handle(InteractionContext context, Decision<? extends TransferAssetIntent> decision) {
    Holdings source = decision.getIntent().from();
    Holdings target = decision.getIntent().to();

    AssetItem item = decision.getIntent().item();
    AssetItem sourceAsset = resolveAsset(source, item);

    source.getAssets().remove(sourceAsset);
    sourceAsset.transferToCommunity(target.getCommunityId());
    target.getAssets().add(sourceAsset);
  }

  private static AssetItem resolveAsset(Holdings holdings, AssetItem item) {
    return holdings.ownedSingleAssetsConformingTo(new NamingSpecification(item.getName()))
        .orElseThrow(IllegalArgumentException::new);
  }

}
