package com.skillserver.repository;

import com.skillserver.domain.entity.SkillRevisionFileEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRevisionFileRepository extends JpaRepository<SkillRevisionFileEntity, Long> {

    List<SkillRevisionFileEntity> findAllBySkillUidAndRevisionOrderByRelativePathAsc(String skillUid, String revision);
}
