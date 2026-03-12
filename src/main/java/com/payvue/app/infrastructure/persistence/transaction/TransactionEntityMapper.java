package com.payvue.app.infrastructure.persistence.transaction;

import com.payvue.core.asset.AssetItem;
import com.payvue.core.asset.AssetItemId;
import com.payvue.app.presentation.rest.community.SystemCommunityProvider;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import com.payvue.core.community.CommunityRepository;
import com.payvue.core.hold.Holdings;
import com.payvue.core.hold.HoldingsRepository;
import com.payvue.app.domain.asset.specification.IdSpecification;
import com.payvue.core.transaction.command.Transaction;
import com.payvue.core.transaction.command.TransactionId;
import com.payvue.supporting.user.command.UserId;
import java.util.UUID;
import org.springframework.stereotype.Component;


@Component
class TransactionEntityMapper {

  private final CommunityRepository communityRepository;
  private final HoldingsRepository holdingsRepository;
  private final SystemCommunityProvider systemCommunityProvider;

  public TransactionEntityMapper(
          CommunityRepository communityRepository,
          HoldingsRepository holdingsRepository,
          SystemCommunityProvider systemCommunityProvider
  ) {
    this.communityRepository = communityRepository;
    this.holdingsRepository = holdingsRepository;
    this.systemCommunityProvider = systemCommunityProvider;
  }

  public TransactionEntity toEntity(Transaction tx) {
    return new TransactionEntity(
            tx.getId().toString(),
            tx.type(),
            tx.assetType(),
            tx.community().getId().toString(),
            tx.from() == null ? null : tx.from().getId().toString(),
            tx.to() == null ? null : tx.to().getId().toString(),
            tx.item().getId().toString(),
            tx.actorId().toString(),
            tx.amount(),
            tx.description(),
            tx.category(),
            tx.meta(),
            tx.interactionState(),
            tx.occurredAt()
    );
  }

  public Transaction toDomain(TransactionEntity entity) {
    Community from = resolveCommunity(entity.getFromCommunityId(), entity.getCommunityId());
    Community to = resolveCommunity(entity.getToCommunityId(), entity.getCommunityId());
    Community ownerCommunity = from;
    if (to != null && !isSystemCommunity(to)) {
      ownerCommunity = to;
    }
    Holdings holdings = holdingsRepository.findByCommunity(ownerCommunity);
    AssetItem item = holdingsRepository.findByCommunityBySepcification(
                    ownerCommunity,
                    new IdSpecification(AssetItemId.of(UUID.fromString(entity.getAssetItemId())))
            )
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("자산 항목을 찾을 수 없습니다."));

    return Transaction.reconstitute(
            new TransactionId(entity.getId()),
            entity.getType(),
            entity.getAssetType(),
            from,
            to,
            item,
            entity.getAmount(),
            UserId.of(UUID.fromString(entity.getActorId())),
            entity.getDescription(),
            entity.getCategory(),
            entity.getInteractionState(),
            entity.getOccurredAt(),
            entity.getMeta()
    );
  }

  private Community resolveCommunity(String primaryId, String fallbackId) {
    String targetId = primaryId == null || primaryId.isBlank() ? fallbackId : primaryId;
    if (targetId == null || targetId.isBlank()) {
      return null;
    }
    if (systemCommunityProvider != null && systemCommunityProvider.isSystemCommunity(targetId)) {
      return systemCommunityProvider.getOrCreate();
    }
    return communityRepository.findById(CommunityId.of(UUID.fromString(targetId)))
        .orElseThrow(() -> new IllegalArgumentException("커뮤니티를 찾을 수 없습니다."));
  }

  private boolean isSystemCommunity(Community community) {
    if (community == null || community.getId() == null) {
      return false;
    }
    return systemCommunityProvider != null
        && systemCommunityProvider.isSystemCommunity(community.getId().toString());
  }
}
