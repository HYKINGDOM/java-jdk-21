package com.skillserver.domain.entity;

import com.skillserver.domain.enums.ResourceRole;
import com.skillserver.domain.enums.ResourceType;
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
@Table(name = "resource_roles", uniqueConstraints = {
    @UniqueConstraint(name = "uq_resource_role_user_resource", columnNames = {"user_id", "resource_type", "resource_id"})
})
public class ResourceRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(nullable = false, length = 100)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceRole role;

    @Column(nullable = false, length = 100)
    private String assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
