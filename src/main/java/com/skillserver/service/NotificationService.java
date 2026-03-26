package com.skillserver.service;

import com.skillserver.common.exception.ForbiddenException;
import com.skillserver.common.exception.NotFoundException;
import com.skillserver.domain.entity.NotificationEntity;
import com.skillserver.domain.enums.NotificationType;
import com.skillserver.dto.skill.NotificationResponse;
import com.skillserver.repository.NotificationRepository;
import com.skillserver.security.AppUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyUser(Long userId,
                           NotificationType type,
                           String skillUid,
                           String revision,
                           String title,
                           String content) {
        notificationRepository.save(NotificationEntity.builder()
            .userId(userId)
            .type(type)
            .skillUid(skillUid)
            .revision(revision)
            .title(title)
            .content(content)
            .read(false)
            .createdAt(LocalDateTime.now())
            .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(AppUserPrincipal actor) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(actor.getUserId()).stream()
            .map(notification -> new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getSkillUid(),
                notification.getRevision(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id, AppUserPrincipal actor) {
        NotificationEntity notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("通知不存在: " + id));
        if (!notification.getUserId().equals(actor.getUserId())) {
            throw new ForbiddenException("无权操作此通知");
        }
        notification.setRead(true);
        return new NotificationResponse(
            notification.getId(),
            notification.getType().name(),
            notification.getSkillUid(),
            notification.getRevision(),
            notification.getTitle(),
            notification.getContent(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
