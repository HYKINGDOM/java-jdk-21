package com.skillserver.repository;

import com.skillserver.domain.entity.SkillRevisionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRevisionRepository extends JpaRepository<SkillRevisionEntity, Long> {

    List<SkillRevisionEntity> findAllBySkillUidOrderByCommittedAtDesc(String skillUid);

    Optional<SkillRevisionEntity> findBySkillUidAndRevision(String skillUid, String revision);
}
