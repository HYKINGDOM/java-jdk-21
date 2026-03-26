package com.skillserver.web;

import com.skillserver.dto.common.PageResponse;
import com.skillserver.dto.skill.FileNodeResponse;
import com.skillserver.dto.skill.RevisionDetailResponse;
import com.skillserver.dto.skill.SkillDetailResponse;
import com.skillserver.dto.skill.SkillSummaryResponse;
import com.skillserver.dto.skill.TimelineEntryResponse;
import com.skillserver.security.SecurityUtils;
import com.skillserver.service.SkillService;
import com.skillserver.service.model.DownloadPayload;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public PageResponse<SkillSummaryResponse> listSkills(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) String sourceType,
                                                         @RequestParam(required = false) List<String> tags,
                                                         @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return skillService.listSkills(q, sourceType, tags, sortBy, page, size, SecurityUtils.currentUser());
    }

    @GetMapping("/{skillUid}")
    public SkillDetailResponse getSkill(@PathVariable String skillUid) {
        return skillService.getSkillDetail(skillUid, SecurityUtils.currentUser());
    }

    @GetMapping("/{skillUid}/tree")
    public List<FileNodeResponse> getTree(@PathVariable String skillUid) {
        return skillService.getTree(skillUid, SecurityUtils.currentUser());
    }

    @GetMapping("/{skillUid}/timeline")
    public List<TimelineEntryResponse> getTimeline(@PathVariable String skillUid) {
        return skillService.getTimeline(skillUid, SecurityUtils.currentUser());
    }

    @GetMapping("/{skillUid}/timeline/{revision}")
    public RevisionDetailResponse getRevision(@PathVariable String skillUid, @PathVariable String revision) {
        return skillService.getRevisionDetail(skillUid, revision, SecurityUtils.currentUser());
    }

    @GetMapping("/{skillUid}/download")
    public ResponseEntity<byte[]> download(@PathVariable String skillUid) {
        DownloadPayload payload = skillService.download(skillUid, SecurityUtils.currentUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(payload.fileName()).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(payload.bytes());
    }
}
