package com.skillserver.repository;

import com.skillserver.domain.entity.SkillCurrentDocEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillCurrentDocRepository extends JpaRepository<SkillCurrentDocEntity, Long> {

    Optional<SkillCurrentDocEntity> findBySkillUid(String skillUid);

    List<SkillCurrentDocEntity> findAllBySkillUidIn(List<String> skillUids);
}
