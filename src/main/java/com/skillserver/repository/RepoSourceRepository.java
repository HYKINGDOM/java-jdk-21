package com.skillserver.repository;

import com.skillserver.domain.entity.RepoSourceEntity;
import com.skillserver.domain.enums.RepoStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoSourceRepository extends JpaRepository<RepoSourceEntity, Long> {

    Optional<RepoSourceEntity> findByNormalizedRepoUrlAndBranch(String normalizedRepoUrl, String branch);

    List<RepoSourceEntity> findAllByStatus(RepoStatus status);
}
