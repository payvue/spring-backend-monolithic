package com.payvue.app.domain.community;

import java.util.UUID;

import com.payvue.core.community.Community;
import com.payvue.core.community.Participant;
import com.payvue.core.community.RoleDescriptor;
import com.payvue.core.community.policy.CommunityPolicy;

public class Cooperative extends Community {

    private final RoleDescriptor adminRole;
    private final RoleDescriptor memberRole;

    private Cooperative(
        String name,
        String description,
        UUID ownerId,
        RoleDescriptor adminRole,
        RoleDescriptor memberRole
    ) {
        super(name, description, ownerId, adminRole);
        this.adminRole = adminRole;
        this.memberRole = memberRole;
    }

    public static Cooperative create(
        String name,
        String description,
        UUID ownerId,
        RoleDescriptor adminRole,
        RoleDescriptor memberRole
    ) {
        return new Cooperative(name, description, ownerId, adminRole, memberRole);
    }

    @Override
    public String getCommunityType() {
        return CommunityCategory.COOPERATIVE.name();
    }

    @Override
    public void addMember(UUID userId, CommunityPolicy policy) {
        if(policy.isSatisfied(this)) {
            getParticipantsInternal().add(Participant.of(userId, memberRole));
        }

        validateInvariants();
    }

    @Override
    public void addMember(Participant participant, CommunityPolicy policy) {
        if(policy.isSatisfied(this)) {
            getParticipantsInternal().add(participant);
        }

        validateInvariants();
    }

    @Override
    public void removeMember(UUID userId, CommunityPolicy policy) {
        if(policy.isSatisfied(this)) {
            Participant participant = findParticipant(userId)
                    .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다."));

            if (participant.getRole().isAdmin()) {
                throw new IllegalStateException("결정권자는 제거할 수 없습니다.");
            }

            getParticipantsInternal().remove(participant);
            validateInvariants();
        }
    }

    @Override
    public void changeParticipantRole(UUID userId, RoleDescriptor newRole, CommunityPolicy policy) {
        if(policy.isSatisfied(this)) {
            Participant participant = findParticipant(userId)
                    .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다."));

            getParticipantsInternal().remove(participant);
            getParticipantsInternal().add(Participant.of(userId, newRole));
            validateInvariants();
        }
    }

    public void activateMember(UUID userId, CommunityPolicy policy) {
        if (policy.isSatisfied(this)) {
            Participant participant = findParticipant(userId)
                    .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다."));
            participant.activate();
            validateInvariants();
        }
    }

    public void transferOwnership(UUID currentOwnerId, UUID newOwnerId, CommunityPolicy policy) {
        if (policy.isSatisfied(this)) {
            if (isAdmin(currentOwnerId)) {
                try {
                    updateParticipantRole(currentOwnerId, memberRole);
                    updateParticipantRole(newOwnerId, adminRole);
                    validateInvariants();
                } catch (IllegalArgumentException e) {
                    updateParticipantRole(currentOwnerId, adminRole);
                    throw e;
                }
            }
        }
    }

    private void updateParticipantRole(UUID userId, RoleDescriptor newRole) {
        Participant participant = findParticipant(userId)
                .orElseThrow(() -> new IllegalArgumentException("참여자를 찾을 수 없습니다."));

        getParticipantsInternal().remove(participant);
        getParticipantsInternal().add(Participant.of(userId, newRole));
    }

}
