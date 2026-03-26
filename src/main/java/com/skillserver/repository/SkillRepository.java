package com.skillserver.repository;

import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.enums.SkillStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {

    Optional<SkillEntity> findBySkillUid(String skillUid);

    List<SkillEntity> findAllBySkillUidIn(Collection<String> skillUids);

    List<SkillEntity> findAllByStatus(SkillStatus status);

    List<SkillEntity> findAllByRepoSource_Id(Long repoSourceId);
}
