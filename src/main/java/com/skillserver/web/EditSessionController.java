package com.skillserver.web;

import com.skillserver.dto.skill.EditSessionHeartbeatRequest;
import com.skillserver.dto.skill.EditSessionRequest;
import com.skillserver.dto.skill.EditSessionResponse;
import com.skillserver.security.SecurityUtils;
import com.skillserver.service.EditSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skills/private/{skillUid}/edit-session")
@RequiredArgsConstructor
public class EditSessionController {

    private final EditSessionService editSessionService;

    @PostMapping
    public EditSessionResponse create(@PathVariable String skillUid, @RequestBody(required = false) EditSessionRequest request) {
        boolean forceTakeover = request != null && Boolean.TRUE.equals(request.forceTakeover());
        return editSessionService.create(skillUid, forceTakeover, SecurityUtils.currentUser());
    }

    @PostMapping("/heartbeat")
    public EditSessionResponse heartbeat(@PathVariable String skillUid, @Valid @RequestBody EditSessionHeartbeatRequest request) {
        return editSessionService.heartbeat(skillUid, request.lockToken(), SecurityUtils.currentUser());
    }

    @DeleteMapping
    public void release(@PathVariable String skillUid, @RequestParam String lockToken) {
        editSessionService.release(skillUid, lockToken, SecurityUtils.currentUser());
    }
}
