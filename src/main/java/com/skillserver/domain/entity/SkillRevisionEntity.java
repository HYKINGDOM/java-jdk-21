package com.skillserver.domain.entity;

import com.skillserver.domain.enums.SourceType;
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
@Table(name = "skill_revisions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_skill_revision_skill_uid_revision", columnNames = {"skill_uid", "revision"})
})
public class SkillRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 80)
    private String revision;

    @Column(length = 80)
    private String parentRevision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @Column(nullable = false, length = 100)
    private String committedBy;

    @Column(nullable = false)
    private LocalDateTime committedAt;

    @Column(nullable = false)
    private boolean hasSkillMdChange;

    @Column(nullable = false)
    private boolean hasOtherFilesChange;

    @Column(nullable = false, length = 40)
    private String changeScope;

    @Column(nullable = false)
    private Integer changedFilesCount;

    @Column(nullable = false)
    private Integer addedFilesCount;

    @Column(nullable = false)
    private Integer modifiedFilesCount;

    @Column(nullable = false)
    private Integer deletedFilesCount;

    @Column(nullable = false)
    private boolean affectsSearch;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 128)
    private String treeFingerprint;

    @Column(length = 128)
    private String skillMdFingerprint;

    @Column(columnDefinition = "TEXT")
    private String originSnapshotJson;

    @Column(columnDefinition = "TEXT")
    private String manifestJson;

    @Column(columnDefinition = "TEXT")
    private String changeSummaryJson;
}
