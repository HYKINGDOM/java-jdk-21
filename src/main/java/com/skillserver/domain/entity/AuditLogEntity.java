package com.skillserver.domain.entity;

import com.skillserver.domain.enums.ActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String traceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Long actorId;

    @Column(length = 100)
    private String actorName;

    @Column(length = 64)
    private String actorIp;

    @Column(nullable = false, length = 100)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionType actionType;

    @Column(nullable = false, length = 30)
    private String targetType;

    @Column(length = 100)
    private String targetId;

    @Column(length = 255)
    private String targetName;

    @Column(columnDefinition = "TEXT")
    private String detailJson;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Long durationMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
