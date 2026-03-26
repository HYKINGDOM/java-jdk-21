package com.skillserver.domain.entity;

import com.skillserver.common.entity.BaseEntity;
import com.skillserver.domain.enums.RepoStatus;
import com.skillserver.domain.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "repo_sources", uniqueConstraints = {
    @UniqueConstraint(name = "uq_repo_normalized_branch", columnNames = {"normalized_repo_url", "branch"})
})
public class RepoSourceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String repoUrl;

    @Column(nullable = false, length = 500, name = "normalized_repo_url")
    private String normalizedRepoUrl;

    @Column(nullable = false, length = 120)
    private String branch;

    @Column(length = 120)
    private String defaultBranch;

    @Column(length = 50)
    private String authType;

    @Column(length = 200)
    private String authSecretRef;

    @Column(nullable = false)
    private boolean syncEnabled;

    @Column(length = 120)
    private String syncCron;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus syncStatus;

    private LocalDateTime lastSyncedAt;

    @Column(length = 80)
    private String lastSyncedCommit;

    @Column(length = 128)
    private String lastScannedTreeFingerprint;

    @Column(length = 80)
    private String lastRevision;

    private Long lastSyncJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepoStatus status;

    @Column(nullable = false, length = 100)
    private String createdBy;
}
