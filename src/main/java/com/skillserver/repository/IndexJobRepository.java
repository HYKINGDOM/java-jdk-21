package com.skillserver.repository;

import com.skillserver.domain.entity.IndexJobEntity;
import com.skillserver.domain.enums.JobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexJobRepository extends JpaRepository<IndexJobEntity, Long> {

    List<IndexJobEntity> findAllByStatusOrderByCreatedAtAsc(JobStatus status);
}
