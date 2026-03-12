package com.payvue.app.infrastructure.persistence.community;

import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityId;
import java.util.Optional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.payvue.core.community.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JpaCommunityRepository implements CommunityRepository {

  private final CommunityJpaRepository communityJpaRepository;
  private final CommunityEntityMapper communityEntityMapper;

  @Override
  @Transactional
  public Community save(Community community) {
    CommunityTBL entity = communityEntityMapper.toEntity(community);
    if (community.getId() != null && communityJpaRepository.existsById(community.getId().getValue())) {
      communityJpaRepository.deleteParticipantsByCommunityId(community.getId().getValue());
      CommunityTBL saved = communityJpaRepository.save(entity);
      return communityEntityMapper.toDomain(saved);
    }
    CommunityTBL saved = communityJpaRepository.save(entity);
    return communityEntityMapper.toDomain(saved);
  }

  @Override
  public Optional<Community> findById(CommunityId id) {
    return communityJpaRepository.findById(id.getValue()).map(communityEntityMapper::toDomain);
  }

  @Override
  public List<Community> findAllByIds(Set<CommunityId> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    Set<UUID> values = ids.stream().map(CommunityId::getValue).collect(java.util.stream.Collectors.toSet());
    return communityJpaRepository.findAllByIdInWithParticipants(values).stream()
        .map(communityEntityMapper::toDomain)
        .toList();
  }

  @Override
  public void delete(Community community) {
    if (community == null) {
      return;
    }
    communityJpaRepository.deleteById(community.getId().getValue());
  }

  @Override
  public boolean existsById(CommunityId id) {
    return communityJpaRepository.existsById(id.getValue());
  }

  @Override
  public boolean existsByName(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    return communityJpaRepository.existsByName(name.trim());
  }

}
