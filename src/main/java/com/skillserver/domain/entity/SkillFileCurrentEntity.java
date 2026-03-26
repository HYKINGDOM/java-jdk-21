package com.skillserver.domain.entity;

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
@Table(name = "skill_files_current", uniqueConstraints = {
    @UniqueConstraint(name = "uq_skill_file_current", columnNames = {"skill_uid", "relative_path"})
})
public class SkillFileCurrentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 80)
    private String revision;

    @Column(nullable = false, length = 500)
    private String relativePath;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private boolean dir;

    @Column(length = 32)
    private String fileExt;

    private Long sizeBytes;

    @Column(length = 200)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PreviewMode previewMode;

    @Column(length = 128)
    private String sha256;

    @Column(nullable = false)
    private Integer sortOrder;
}
