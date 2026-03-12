package com.payvue.app.infrastructure.persistence.relation;

import com.payvue.app.infrastructure.persistence.community.CommunityEntityMapper;
import com.payvue.app.infrastructure.persistence.community.CommunityTBL;
import com.payvue.app.infrastructure.persistence.community.CommunityJpaRepository;
import com.payvue.core.community.Community;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.payvue.core.relation.CommunityRelation;
import com.payvue.core.relation.RelationType;
import com.payvue.core.relation.CommunityRelationRepository;
import com.payvue.core.relation.ConnectionStatus;
import com.payvue.core.relation.RelationId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaCommunityRelationRepository implements CommunityRelationRepository {

  private final CommunityRelationJpaRepository relationJpaRepository;
  private final CommunityJpaRepository communityJpaRepository;
  private final CommunityRelationMapper relationMapper;
  private final CommunityEntityMapper communityEntityMapper;

  public JpaCommunityRelationRepository(
      CommunityRelationJpaRepository relationJpaRepository,
      CommunityJpaRepository communityJpaRepository,
      CommunityRelationMapper relationMapper,
      CommunityEntityMapper communityEntityMapper
  ) {
    this.relationJpaRepository = relationJpaRepository;
    this.communityJpaRepository = communityJpaRepository;
    this.relationMapper = relationMapper;
    this.communityEntityMapper = communityEntityMapper;
  }

  @Override
  @Transactional
  public void save(CommunityRelation relation) {
    CommunityRelationTBL entity = relationMapper.toEntity(relation);
    if (relationJpaRepository.existsById(entity.getId())) {
      CommunityRelationTBL existing = relationJpaRepository.findById(entity.getId()).orElse(null);
      if (existing == null) {
        relationJpaRepository.save(entity);
      } else {
        existing.updateStatus(entity.getStatus());
        relationJpaRepository.save(existing);
      }
    } else {
      relationJpaRepository.save(entity);
    }
  }

  @Override
  public Optional<CommunityRelation> findById(RelationId id) {
    return relationJpaRepository.findById(id.getValue()).flatMap(this::toDomain);
  }

  @Override
  public void delete(CommunityRelation relation) {
    if (relation == null) {
      return;
    }
    relationJpaRepository.deleteById(relation.getId().getValue());
  }

  @Override
  public Optional<CommunityRelation> findRelationBetween(Community from, Community to) {
    if (from == null || to == null) {
      return Optional.empty();
    }
    return relationJpaRepository
        .findByFromCommunityIdAndToCommunityId(from.getId().getValue(), to.getId().getValue())
        .flatMap(this::toDomain);
  }

  @Override
  public boolean existsRelationBetween(Community from, Community to) {
    if (from == null || to == null) {
      return false;
    }
    return relationJpaRepository.existsByFromCommunityIdAndToCommunityId(
        from.getId().getValue(),
        to.getId().getValue()
    );
  }

  @Override
  public List<CommunityRelation> findRelationsFrom(Community community, int page, int sz) {
    if (community == null) {
      return List.of();
    }
    return relationJpaRepository
        .findByFromCommunityId(community.getId().getValue(), PageRequest.of(page, sz))
        .stream()
        .map(this::toDomain)
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  @Override
  public List<CommunityRelation> findRelationsByType(Community community, RelationType type) {
    if (community == null || type == null) {
      return List.of();
    }
    return relationJpaRepository
        .findByFromCommunityIdAndRelationType(community.getId().getValue(), type)
        .stream()
        .map(this::toDomain)
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  @Override
  public List<CommunityRelation> findPendingRelationsOf(Community community) {
    return findRelationsByStatus(community, ConnectionStatus.PENDING);
  }

  @Override
  public List<CommunityRelation> findActiveRelationsOf(Community community) {
    return findRelationsByStatus(community, ConnectionStatus.ACTIVE);
  }

  private Optional<CommunityRelation> toDomain(CommunityRelationTBL entity) {
    Optional<CommunityTBL> fromEntity = communityJpaRepository.findById(entity.getFromCommunityId());
    Optional<CommunityTBL> toEntity = communityJpaRepository.findById(entity.getToCommunityId());
    if (fromEntity.isEmpty() || toEntity.isEmpty()) {
      return Optional.empty();
    }
    Community from = communityEntityMapper.toDomain(fromEntity.get());
    Community to = communityEntityMapper.toDomain(toEntity.get());
    return Optional.ofNullable(relationMapper.toDomain(entity, from, to));
  }

  private List<CommunityRelation> findRelationsByStatus(Community community, ConnectionStatus status) {
    if (community == null || status == null) {
      return List.of();
    }
    UUID communityId = community.getId().getValue();
    return relationJpaRepository
        .findByCommunityIdAndStatus(communityId, status)
        .stream()
        .map(this::toDomain)
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }
}
