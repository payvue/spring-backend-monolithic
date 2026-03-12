package com.payvue.app.infrastructure.persistence.transaction;

import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.query.TransactionHistoryPage;
import com.payvue.core.transaction.query.TransactionHistoryView;
import com.payvue.core.transaction.query.TransactionQueryRepository;
import com.payvue.shared.Period;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.payvue.app.infrastructure.persistence.user.entity.UserEntity;
import com.payvue.app.infrastructure.persistence.user.jpa.UserEntityJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTransactionQueryRepository implements TransactionQueryRepository {

  private final TransactionJpaRepository jpaRepository;
  private final TransactionHistoryViewMapper viewMapper;
  private final UserEntityJpaRepository userRepository;

  public JpaTransactionQueryRepository(
      TransactionJpaRepository jpaRepository,
      TransactionHistoryViewMapper viewMapper,
      UserEntityJpaRepository userRepository
  ) {
    this.jpaRepository = jpaRepository;
    this.viewMapper = viewMapper;
    this.userRepository = userRepository;
  }

  @Override
  public TransactionHistoryPage findPersonal(
      String actorId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  ) {
    List<TransactionEntity> source = jpaRepository.findByActorId(actorId);
    return toPage(source, assetType, transactionType, pageable, period, null);
  }

  @Override
  public TransactionHistoryPage findCommunity(
      String communityId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  ) {
    List<TransactionEntity> source = jpaRepository.findByFromCommunityIdOrToCommunityId(
        communityId,
        communityId
    );
    return toPage(source, assetType, transactionType, pageable, period, communityId);
  }

  private TransactionHistoryPage toPage(
      List<TransactionEntity> source,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period,
      String viewerCommunityId
  ) {
    Instant from = period == null ? null : period.from();
    Instant to = period == null ? null : period.to();
    List<TransactionEntity> filtered = source.stream()
        .filter(entity -> assetType == null || "ALL".equals(assetType) || assetType.equals(entity.getAssetType()))
        .filter(entity -> transactionType == null || transactionType == TransactionType.ALL || transactionType == entity.getType())
        .filter(entity -> from == null || entity.getOccurredAt() == null || !entity.getOccurredAt().isBefore(from))
        .filter(entity -> to == null || entity.getOccurredAt() == null || !entity.getOccurredAt().isAfter(to))
        .sorted(Comparator.comparing(TransactionEntity::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .toList();

    long total = filtered.size();
    List<TransactionEntity> pageItems = slice(filtered, pageable);
    Map<String, String> paidByByActorId = resolvePaidBy(pageItems);
    List<TransactionHistoryView> views = pageItems.stream()
        .map(entity -> viewMapper.toView(
            entity,
            viewerCommunityId,
            paidByByActorId.get(entity.getActorId())
        ))
        .collect(Collectors.toList());

    return new TransactionHistoryPage(views, total);
  }

  private Map<String, String> resolvePaidBy(List<TransactionEntity> items) {
    if (items == null || items.isEmpty()) {
      return Map.of();
    }
    List<String> actorIds = items.stream()
        .map(TransactionEntity::getActorId)
        .filter(id -> id != null && !id.isBlank())
        .distinct()
        .toList();
    if (actorIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(actorIds).stream()
        .filter(entity -> entity.getDisplayName() != null && !entity.getDisplayName().isBlank())
        .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName));
  }

  private List<TransactionEntity> slice(List<TransactionEntity> source, Pageable pageable) {
    if (pageable == null || pageable.isUnpaged()) {
      return source;
    }
    int fromIndex = Math.max(0, pageable.getPageNumber()) * pageable.getPageSize();
    if (fromIndex >= source.size()) {
      return List.of();
    }
    int toIndex = Math.min(source.size(), fromIndex + pageable.getPageSize());
    return source.subList(fromIndex, toIndex);
  }

}
