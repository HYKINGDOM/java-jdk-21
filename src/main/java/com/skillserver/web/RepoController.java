package com.skillserver.web;

import com.skillserver.dto.skill.RepoImportRequest;
import com.skillserver.dto.skill.RepoResponse;
import com.skillserver.security.SecurityUtils;
import com.skillserver.service.GitRepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repos")
@RequiredArgsConstructor
public class RepoController {

    private final GitRepoService gitRepoService;

    @PostMapping("/import")
    public RepoResponse importRepo(@Valid @RequestBody RepoImportRequest request) {
        return gitRepoService.importRepo(request, SecurityUtils.currentUser());
    }

    @PostMapping("/{repoId}/sync")
    public RepoResponse syncRepo(@PathVariable Long repoId) {
        return gitRepoService.syncRepo(repoId, SecurityUtils.currentUser());
    }

    @DeleteMapping("/{repoId}")
    public void deleteRepo(@PathVariable Long repoId) {
        gitRepoService.deleteRepo(repoId, SecurityUtils.currentUser());
    }
}
