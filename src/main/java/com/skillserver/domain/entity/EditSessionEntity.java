package com.skillserver.domain.entity;

import com.skillserver.domain.enums.EditSessionStatus;
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
@Table(name = "edit_sessions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_edit_session_skill_uid", columnNames = "skill_uid")
})
public class EditSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String skillUid;

    @Column(nullable = false, length = 100)
    private String lockToken;

    @Column(nullable = false, length = 100)
    private String lockedBy;

    @Column(nullable = false)
    private Long lockedByUserId;

    @Column(nullable = false, length = 80)
    private String baseRevision;

    @Column(length = 128)
    private String baseTreeFingerprint;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private LocalDateTime heartbeatAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EditSessionStatus status;

    @Column(length = 100)
    private String takeoverBy;

    private LocalDateTime takeoverAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
