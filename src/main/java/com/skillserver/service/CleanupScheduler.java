package com.skillserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final EditSessionService editSessionService;
    private final SkillService skillService;

    @Scheduled(fixedDelay = 60000)
    public void expireEditSessions() {
        int expired = editSessionService.expireSessions();
        if (expired > 0) {
            log.info("Expired {} edit sessions", expired);
        }
    }

    @Scheduled(cron = "${skill-server.cleanup.purge-cron}")
    public void purgeDeletedSkills() {
        int purged = skillService.purgeSoftDeletedSkills();
        if (purged > 0) {
            log.info("Purged {} deleted skills", purged);
        }
    }
}
