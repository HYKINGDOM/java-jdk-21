package com.skillserver.web;

import com.skillserver.dto.skill.CreatePrivateSkillRequest;
import com.skillserver.dto.skill.SkillDetailResponse;
import com.skillserver.dto.skill.UpdatePrivateSkillRequest;
import com.skillserver.security.SecurityUtils;
import com.skillserver.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/skills/private")
@RequiredArgsConstructor
public class PrivateSkillController {

    private final SkillService skillService;

    @PostMapping("/create")
    public SkillDetailResponse create(@Valid @RequestBody CreatePrivateSkillRequest request) {
        return skillService.createPrivateSkill(request, SecurityUtils.currentUser());
    }

    @PostMapping(value = "/upload-skill-md", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkillDetailResponse uploadSkillMarkdown(@RequestPart("file") MultipartFile file) {
        return skillService.uploadSkillMarkdown(file, SecurityUtils.currentUser());
    }

    @PostMapping(value = "/upload-folder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkillDetailResponse uploadFolder(@RequestPart("file") MultipartFile file) {
        return skillService.uploadFolder(file, SecurityUtils.currentUser());
    }

    @PutMapping("/{skillUid}")
    public SkillDetailResponse update(@PathVariable String skillUid, @Valid @RequestBody UpdatePrivateSkillRequest request) {
        return skillService.updatePrivateSkill(skillUid, request, SecurityUtils.currentUser());
    }

    @DeleteMapping("/{skillUid}")
    public void delete(@PathVariable String skillUid) {
        skillService.deletePrivateSkill(skillUid, SecurityUtils.currentUser());
    }
}
