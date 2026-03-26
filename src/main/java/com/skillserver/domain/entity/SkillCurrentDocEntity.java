package com.skillserver.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "skill_current_docs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_skill_current_doc_skill_uid", columnNames = "skill_uid")
})
public class SkillCurrentDocEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String tagsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyMarkdown;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyPlaintext;

    @Column(nullable = false, length = 128)
    private String contentHash;

    @Column(nullable = false, length = 80)
    private String revision;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
