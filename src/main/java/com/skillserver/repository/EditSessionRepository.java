package com.skillserver.repository;

import com.skillserver.domain.entity.EditSessionEntity;
import com.skillserver.domain.enums.EditSessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditSessionRepository extends JpaRepository<EditSessionEntity, Long> {

    Optional<EditSessionEntity> findBySkillUid(String skillUid);

    Optional<EditSessionEntity> findBySkillUidAndStatus(String skillUid, EditSessionStatus status);

    Optional<EditSessionEntity> findBySkillUidAndLockToken(String skillUid, String lockToken);

    List<EditSessionEntity> findAllByStatusAndExpireAtBefore(EditSessionStatus status, LocalDateTime expireAt);
}
