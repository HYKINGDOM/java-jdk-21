package com.skillserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.UUID;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivateSkillFlowIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        Path root = Path.of("target", "test-runs", "private-" + RUN_ID);
        registry.add("skill-server.workspace.root", () -> root.toString());
        registry.add("skill-server.workspace.private-skills-dir", () -> root.resolve("private-skills").toString());
        registry.add("skill-server.workspace.mirrors-dir", () -> root.resolve("mirrors").toString());
        registry.add("skill-server.workspace.temp-dir", () -> root.resolve("tmp").toString());
        registry.add("skill-server.workspace.package-dir", () -> root.resolve("packages").toString());
        registry.add("skill-server.workspace.search-index-dir", () -> root.resolve("index").toString());
    }

    @Test
    void shouldCreateUpdateSearchAndNotifyPrivateSkill() throws Exception {
        String createPayload = """
            {
              "name": "Skill Server Java",
              "description": "Java 21 Spring Boot version",
              "tags": ["java", "spring"],
              "files": [
                {"path": "README.md", "content": "# README\\n\\nprivate skill readme"}
              ]
            }
            """;

        JsonNode created = readJson(mockMvc.perform(post("/skills/private/create")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        String skillUid = created.get("skillUid").asText();
        String baseRevision = created.get("currentRevision").asText();

        JsonNode session = readJson(mockMvc.perform(post("/skills/private/" + skillUid + "/edit-session")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        String lockToken = session.get("lockToken").asText();
        String updatePayload = """
            {
              "lockToken": "%s",
              "baseRevision": "%s",
              "summary": "update markdown",
              "upserts": [
                {"path": "SKILL.md", "content": "# Skill Server Java\\n\\n## 用途\\n\\nupdated spring boot skill\\n标签: java, spring, updated"}
              ]
            }
            """.formatted(lockToken, baseRevision);

        mockMvc.perform(put("/skills/private/" + skillUid)
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk());

        JsonNode searchResult = readJson(mockMvc.perform(get("/skills")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                .param("q", "updated"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());

        assertThat(searchResult.get("total").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode timeline = readJson(mockMvc.perform(get("/skills/" + skillUid + "/timeline")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertThat(timeline.size()).isEqualTo(2);

        JsonNode notifications = readJson(mockMvc.perform(get("/notifications")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertThat(notifications.size()).isGreaterThanOrEqualTo(2);
    }

    private JsonNode readJson(String content) throws Exception {
        return objectMapper.readTree(content);
    }
}
