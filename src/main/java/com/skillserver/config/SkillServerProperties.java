package com.skillserver.config;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "skill-server")
public class SkillServerProperties {

    private Workspace workspace = new Workspace();
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Workspace {

        private Path root = Path.of("./workspace");
        private Path privateSkillsDir = Path.of("./workspace/private-skills");
        private Path mirrorsDir = Path.of("./workspace/mirrors");
        private Path tempDir = Path.of("./workspace/tmp");
        private Path packageDir = Path.of("./workspace/packages");
        private Path searchIndexDir = Path.of("./data/index");
    }

    @Getter
    @Setter
    public static class Cleanup {

        private int deletedRetentionDays = 7;
        private int editSessionTimeoutMinutes = 20;
        private String purgeCron = "0 0 3 * * *";
        private String syncCron = "0 */30 * * * *";
    }
}
