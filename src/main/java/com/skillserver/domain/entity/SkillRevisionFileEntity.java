package com.skillserver.domain.entity;

import com.skillserver.domain.enums.ChangeType;
import com.skillserver.domain.enums.PreviewMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "skill_revision_files", uniqueConstraints = {
    @UniqueConstraint(name = "uq_skill_revision_file", columnNames = {"skill_uid", "revision", "relative_path"})
})
public class SkillRevisionFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 80)
    private String revision;

    @Column(nullable = false, length = 500)
    private String relativePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeType changeType;

    @Column(nullable = false)
    private boolean skillMd;

    @Column(length = 32)
    private String fileExt;

    private Long sizeBefore;
    private Long sizeAfter;

    @Column(length = 128)
    private String oldFileSha256;

    @Column(length = 128)
    private String newFileSha256;

    private Long fileSize;

    @Column(length = 128)
    private String hashBefore;

    @Column(length = 128)
    private String hashAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PreviewMode previewMode;
}
