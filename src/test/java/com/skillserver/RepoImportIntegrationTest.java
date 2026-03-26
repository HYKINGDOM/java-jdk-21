package com.skillserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepoImportIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        Path root = Path.of("target", "test-runs", "repo-" + RUN_ID);
        registry.add("skill-server.workspace.root", () -> root.toString());
        registry.add("skill-server.workspace.private-skills-dir", () -> root.resolve("private-skills").toString());
        registry.add("skill-server.workspace.mirrors-dir", () -> root.resolve("mirrors").toString());
        registry.add("skill-server.workspace.temp-dir", () -> root.resolve("tmp").toString());
        registry.add("skill-server.workspace.package-dir", () -> root.resolve("packages").toString());
        registry.add("skill-server.workspace.search-index-dir", () -> root.resolve("index").toString());
    }

    @Test
    void shouldImportAndSyncGitRepository() throws Exception {
        Path repoPath = Files.createTempDirectory(Path.of("target"), "local-repo-");
        initGitRepo(repoPath, "# Demo Skill\n\n## 用途\n\ngit imported skill\n标签: git, java");

        String repoUrl = repoPath.toAbsolutePath().toString().replace("\\", "/");
        String importPayload = """
            {
              "name": "demo-repo",
              "repoUrl": "%s",
              "branch": "main",
              "syncEnabled": false
            }
            """.formatted(repoUrl);

        JsonNode repo = readJson(mockMvc.perform(post("/repos/import")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(importPayload))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/skills")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .param("sourceType", "GIT"))
            .andExpect(status().isOk());

        JsonNode skills = readJson(mockMvc.perform(get("/skills")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .param("q", "imported"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        assertThat(skills.get("total").asInt()).isGreaterThanOrEqualTo(1);
    }

    private void initGitRepo(Path repoPath, String skillMarkdown) throws IOException, GitAPIException {
        Files.createDirectories(repoPath);
        Files.writeString(repoPath.resolve("SKILL.md"), skillMarkdown, StandardCharsets.UTF_8);
        try (Git git = Git.init().setInitialBranch("main").setDirectory(repoPath.toFile()).call()) {
            git.add().addFilepattern("SKILL.md").call();
            git.commit().setMessage("initial commit").call();
        }
    }

    private JsonNode readJson(String content) throws Exception {
        return objectMapper.readTree(content);
    }
}
