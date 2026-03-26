package com.skillserver.service;

import com.skillserver.common.exception.ForbiddenException;
import com.skillserver.domain.entity.ResourceRoleEntity;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.enums.ResourceRole;
import com.skillserver.domain.enums.ResourceType;
import com.skillserver.domain.enums.SystemRole;
import com.skillserver.repository.ResourceRoleRepository;
import com.skillserver.security.AppUserPrincipal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private static final EnumSet<ResourceRole> READABLE = EnumSet.of(ResourceRole.OWNER, ResourceRole.MAINTAINER,
        ResourceRole.EDITOR, ResourceRole.VIEWER);
    private static final EnumSet<ResourceRole> EDITABLE = EnumSet.of(ResourceRole.OWNER, ResourceRole.MAINTAINER,
        ResourceRole.EDITOR);
    private static final EnumSet<ResourceRole> DELETABLE = EnumSet.of(ResourceRole.OWNER, ResourceRole.MAINTAINER);

    private final ResourceRoleRepository resourceRoleRepository;

    public boolean canReadSkill(AppUserPrincipal actor, SkillEntity skill) {
        if (isAdmin(actor)) {
            return true;
        }
        if (skill.getSourceType().name().equals("GIT")) {
            return true;
        }
        return roleOf(actor, ResourceType.SKILL, skill.getSkillUid())
            .map(READABLE::contains)
            .orElse(false);
    }

    public boolean canEditSkill(AppUserPrincipal actor, SkillEntity skill) {
        return isAdmin(actor) || roleOf(actor, ResourceType.SKILL, skill.getSkillUid()).map(EDITABLE::contains).orElse(false);
    }

    public boolean canDeleteSkill(AppUserPrincipal actor, SkillEntity skill) {
        return isAdmin(actor) || roleOf(actor, ResourceType.SKILL, skill.getSkillUid()).map(DELETABLE::contains).orElse(false);
    }

    public boolean canManageRepo(AppUserPrincipal actor, String repoId) {
        return isAdmin(actor) || roleOf(actor, ResourceType.REPO, repoId).map(DELETABLE::contains).orElse(false);
    }

    public void assertCanReadSkill(AppUserPrincipal actor, SkillEntity skill) {
        if (!canReadSkill(actor, skill)) {
            throw new ForbiddenException("无权访问该 Skill");
        }
    }

    public void assertCanEditSkill(AppUserPrincipal actor, SkillEntity skill) {
        if (!canEditSkill(actor, skill)) {
            throw new ForbiddenException("无权编辑该 Skill");
        }
    }

    public void assertCanDeleteSkill(AppUserPrincipal actor, SkillEntity skill) {
        if (!canDeleteSkill(actor, skill)) {
            throw new ForbiddenException("无权删除该 Skill");
        }
    }

    public void assertCanManageRepo(AppUserPrincipal actor, String repoId) {
        if (!canManageRepo(actor, repoId)) {
            throw new ForbiddenException("无权管理该仓库");
        }
    }

    @Transactional
    public void assignOwner(Long userId, ResourceType resourceType, String resourceId, String assignedBy) {
        ResourceRoleEntity role = resourceRoleRepository
            .findByUserIdAndResourceTypeAndResourceId(userId, resourceType, resourceId)
            .orElseGet(() -> ResourceRoleEntity.builder()
                .userId(userId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build());
        role.setRole(ResourceRole.OWNER);
        role.setAssignedBy(assignedBy);
        role.setAssignedAt(LocalDateTime.now());
        role.setCreatedAt(role.getCreatedAt() == null ? LocalDateTime.now() : role.getCreatedAt());
        resourceRoleRepository.save(role);
    }

    private boolean isAdmin(AppUserPrincipal actor) {
        return SystemRole.ADMIN.name().equals(actor.getRole());
    }

    private java.util.Optional<ResourceRole> roleOf(AppUserPrincipal actor, ResourceType resourceType, String resourceId) {
        return resourceRoleRepository.findByUserIdAndResourceTypeAndResourceId(actor.getUserId(), resourceType, resourceId)
            .map(ResourceRoleEntity::getRole);
    }
}
