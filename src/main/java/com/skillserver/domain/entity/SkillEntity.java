package com.skillserver.domain.entity;

import com.skillserver.common.entity.BaseEntity;
import com.skillserver.domain.enums.IndexStatus;
import com.skillserver.domain.enums.OriginMode;
import com.skillserver.domain.enums.SkillStatus;
import com.skillserver.domain.enums.SourceType;
import com.skillserver.domain.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skills", uniqueConstraints = {
    @UniqueConstraint(name = "uq_skill_skill_uid", columnNames = "skill_uid"),
    @UniqueConstraint(name = "uq_skill_repo_relative_dir", columnNames = {"repo_source_id", "relative_dir"})
})
public class SkillEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 160)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_source_id")
    private RepoSourceEntity repoSource;

    @Column(length = 400)
    private String relativeDir;

    @Column(nullable = false, length = 120)
    private String entryFile;

    @Column(length = 80)
    private String currentRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType originType;

    @Column(nullable = false, length = 500)
    private String originLocator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OriginMode originMode;

    @Column(length = 128)
    private String currentTreeFingerprint;

    @Column(length = 128)
    private String currentSkillMdFingerprint;

    @Column(nullable = false)
    private Integer currentManifestVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus syncStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndexStatus indexStatus;

    private LocalDateTime searchUpdatedAt;
    private LocalDateTime indexedAt;
    private LocalDateTime lastSyncAt;
    private LocalDateTime lastIndexAt;

    @Column(nullable = false)
    private Integer favoriteCount;

    @Column(nullable = false)
    private Integer downloadCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SkillStatus status;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false, length = 100)
    private String updatedBy;

    private LocalDateTime deletedAt;
}
