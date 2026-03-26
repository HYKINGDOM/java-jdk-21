package com.skillserver.service;

import com.skillserver.common.exception.ConflictException;
import com.skillserver.common.exception.NotFoundException;
import com.skillserver.domain.entity.EditSessionEntity;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.enums.ActionType;
import com.skillserver.domain.enums.EditSessionStatus;
import com.skillserver.domain.enums.ResourceType;
import com.skillserver.dto.skill.EditSessionResponse;
import com.skillserver.repository.EditSessionRepository;
import com.skillserver.repository.SkillRepository;
import com.skillserver.security.AppUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditSessionService {

    private final EditSessionRepository editSessionRepository;
    private final SkillRepository skillRepository;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final com.skillserver.config.SkillServerProperties properties;

    @Transactional
    public EditSessionResponse create(String skillUid, boolean forceTakeover, AppUserPrincipal actor) {
        long start = System.currentTimeMillis();
        SkillEntity skill = skillRepository.findBySkillUid(skillUid)
            .orElseThrow(() -> new NotFoundException("Skill 不存在: " + skillUid));
        accessControlService.assertCanEditSkill(actor, skill);
        EditSessionEntity current = editSessionRepository.findBySkillUid(skillUid).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (current != null && current.getStatus() == EditSessionStatus.ACTIVE && current.getExpireAt().isAfter(now)) {
            if (current.getLockedByUserId().equals(actor.getUserId())) {
                current.setHeartbeatAt(now);
                current.setExpireAt(now.plusMinutes(properties.getCleanup().getEditSessionTimeoutMinutes()));
                auditLogService.success(actor, "edit_session_reuse", ActionType.WRITE, "skill", skillUid, skill.getSlug(),
                    java.util.Map.of("resourceType", ResourceType.SKILL.name()), System.currentTimeMillis() - start);
                return toResponse(editSessionRepository.save(current));
            }
            if (!forceTakeover) {
                throw new ConflictException("当前 Skill 正在被其他用户编辑");
            }
            current.setStatus(EditSessionStatus.TAKEN_OVER);
            current.setTakeoverBy(actor.getUsername());
            current.setTakeoverAt(now);
        } else if (current != null && current.getStatus() == EditSessionStatus.ACTIVE) {
            current.setStatus(EditSessionStatus.EXPIRED);
        }

        EditSessionEntity session = current == null ? new EditSessionEntity() : current;
        session.setSkillUid(skillUid);
        session.setLockToken(UUID.randomUUID().toString());
        session.setLockedBy(actor.getUsername());
        session.setLockedByUserId(actor.getUserId());
        session.setBaseRevision(skill.getCurrentRevision());
        session.setBaseTreeFingerprint(skill.getCurrentTreeFingerprint());
        session.setHeartbeatAt(now);
        session.setExpireAt(now.plusMinutes(properties.getCleanup().getEditSessionTimeoutMinutes()));
        session.setStatus(EditSessionStatus.ACTIVE);
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(now);
        }
        EditSessionEntity saved = editSessionRepository.save(session);
        auditLogService.success(actor, "edit_session_create", ActionType.WRITE, "skill", skillUid, skill.getSlug(),
            java.util.Map.of("lockToken", saved.getLockToken()), System.currentTimeMillis() - start);
        return toResponse(saved);
    }

    @Transactional
    public EditSessionResponse heartbeat(String skillUid, String lockToken, AppUserPrincipal actor) {
        EditSessionEntity session = validate(skillUid, lockToken, actor);
        LocalDateTime now = LocalDateTime.now();
        session.setHeartbeatAt(now);
        session.setExpireAt(now.plusMinutes(properties.getCleanup().getEditSessionTimeoutMinutes()));
        return toResponse(session);
    }

    @Transactional
    public void release(String skillUid, String lockToken, AppUserPrincipal actor) {
        EditSessionEntity session = validate(skillUid, lockToken, actor);
        session.setStatus(EditSessionStatus.RELEASED);
        session.setExpireAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public EditSessionEntity validate(String skillUid, String lockToken, AppUserPrincipal actor) {
        EditSessionEntity session = editSessionRepository.findBySkillUidAndLockToken(skillUid, lockToken)
            .orElseThrow(() -> new NotFoundException("编辑锁不存在"));
        if (!session.getLockedByUserId().equals(actor.getUserId()) && !"ADMIN".equals(actor.getRole())) {
            throw new ConflictException("编辑锁不属于当前用户");
        }
        if (session.getStatus() != EditSessionStatus.ACTIVE || session.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("编辑锁已失效");
        }
        return session;
    }

    @Transactional
    public int expireSessions() {
        List<EditSessionEntity> expired = editSessionRepository.findAllByStatusAndExpireAtBefore(EditSessionStatus.ACTIVE, LocalDateTime.now());
        expired.forEach(session -> session.setStatus(EditSessionStatus.EXPIRED));
        return expired.size();
    }

    private EditSessionResponse toResponse(EditSessionEntity session) {
        return new EditSessionResponse(
            session.getSkillUid(),
            session.getLockToken(),
            session.getLockedBy(),
            session.getBaseRevision(),
            session.getStatus().name(),
            session.getExpireAt(),
            session.getHeartbeatAt()
        );
    }
}
