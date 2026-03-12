package com.payvue.app.infrastructure.persistence.relation;

import com.payvue.core.relation.CommunityRelation;
import com.payvue.core.relation.RelationId;
import com.payvue.core.community.Community;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * CommunityRelationMapper - Domain ↔ Entity 변환
 * 
 * MapStruct를 사용한 자동 매핑:
 * - 단순 필드는 자동 매핑
 * - Community는 Repository를 통해 조회 (afterMapping)
 * - 복잡한 변환 로직은 커스텀 메서드로 처리
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommunityRelationMapper {

    /**
     * Entity → Domain 변환
     * 
     * Community 객체는 외부에서 주입받아야 함
     * (Repository에서 조회한 Community 전달)
     * 
     * @param entity JPA Entity
     * @param fromCommunity from_community_id로 조회한 Community
     * @param toCommunity to_community_id로 조회한 Community
     * @return 도메인 모델
     */
    default CommunityRelation toDomain(
            CommunityRelationTBL entity,
            Community fromCommunity,
            Community toCommunity
    ) {
        if (entity == null) {
            return null;
        }

        return CommunityRelation.reconstitute(
                RelationId.of(entity.getId()),
                fromCommunity,
                toCommunity,
                entity.getRelationType(),
                entity.getStatus()
        );
    }

    default CommunityRelationTBL toEntity(CommunityRelation domain) {
        if (domain == null) {
            return null;
        }
        UUID id = domain.getId().getValue();
        UUID fromId = domain.getFrom().getId().getValue();
        UUID toId = domain.getTo().getId().getValue();
        return CommunityRelationTBL.create(
                id,
                fromId,
                toId,
                domain.getRelationType(),
                domain.getStatus()
        );
    }

    /**
     * 도메인 모델의 상태 변경을 Entity에 반영
     * 
     * @param domain 업데이트된 도메인 모델
     * @param entity 기존 Entity
     */
    default void updateEntity(CommunityRelation domain, CommunityRelationTBL entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.updateStatus(domain.getStatus());
    }
}
