package com.skillserver.repository;

import com.skillserver.domain.entity.SkillDownloadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillDownloadRepository extends JpaRepository<SkillDownloadEntity, Long> {

    long countBySkillUid(String skillUid);
}
