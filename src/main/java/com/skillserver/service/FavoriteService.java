package com.skillserver.service;

import com.skillserver.common.exception.ConflictException;
import com.skillserver.common.exception.NotFoundException;
import com.skillserver.domain.entity.SkillEntity;
import com.skillserver.domain.entity.SkillFavoriteEntity;
import com.skillserver.repository.SkillFavoriteRepository;
import com.skillserver.repository.SkillRepository;
import com.skillserver.security.AppUserPrincipal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final SkillRepository skillRepository;
    private final SkillFavoriteRepository favoriteRepository;
    private final AccessControlService accessControlService;

    @Transactional
    public void favorite(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = skillRepository.findBySkillUid(skillUid)
            .orElseThrow(() -> new NotFoundException("Skill 不存在: " + skillUid));
        accessControlService.assertCanReadSkill(actor, skill);
        if (favoriteRepository.findByUserIdAndSkillUid(actor.getUserId(), skillUid).isPresent()) {
            throw new ConflictException("已收藏该 Skill");
        }
        favoriteRepository.save(SkillFavoriteEntity.builder()
            .userId(actor.getUserId())
            .skillUid(skillUid)
            .createdAt(LocalDateTime.now())
            .build());
        skill.setFavoriteCount((int) favoriteRepository.countBySkillUid(skillUid));
    }

    @Transactional
    public void unfavorite(String skillUid, AppUserPrincipal actor) {
        SkillEntity skill = skillRepository.findBySkillUid(skillUid)
            .orElseThrow(() -> new NotFoundException("Skill 不存在: " + skillUid));
        SkillFavoriteEntity favorite = favoriteRepository.findByUserIdAndSkillUid(actor.getUserId(), skillUid)
            .orElseThrow(() -> new NotFoundException("未收藏该 Skill"));
        favoriteRepository.delete(favorite);
        skill.setFavoriteCount((int) favoriteRepository.countBySkillUid(skillUid));
    }
}
