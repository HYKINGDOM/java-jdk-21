package com.skillserver.repository;

import com.skillserver.domain.entity.SyncJobEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobRepository extends JpaRepository<SyncJobEntity, Long> {

    List<SyncJobEntity> findAllByRepoSourceIdOrderByCreatedAtDesc(Long repoSourceId);
}
