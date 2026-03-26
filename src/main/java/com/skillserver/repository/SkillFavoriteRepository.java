package com.skillserver.repository;

import com.skillserver.domain.entity.SkillFavoriteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillFavoriteRepository extends JpaRepository<SkillFavoriteEntity, Long> {

    Optional<SkillFavoriteEntity> findByUserIdAndSkillUid(Long userId, String skillUid);

    List<SkillFavoriteEntity> findAllByUserId(Long userId);

    long countBySkillUid(String skillUid);
}
