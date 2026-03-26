package com.skillserver.repository;

import com.skillserver.domain.entity.NotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
