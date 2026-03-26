package com.skillserver.repository;

import com.skillserver.domain.entity.SkillFileCurrentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillFileCurrentRepository extends JpaRepository<SkillFileCurrentEntity, Long> {

    List<SkillFileCurrentEntity> findAllBySkillUidOrderBySortOrderAsc(String skillUid);

    void deleteAllBySkillUid(String skillUid);
}
