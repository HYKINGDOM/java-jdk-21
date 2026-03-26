package com.skillserver.repository;

import com.skillserver.domain.entity.ResourceRoleEntity;
import com.skillserver.domain.enums.ResourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRoleRepository extends JpaRepository<ResourceRoleEntity, Long> {

    Optional<ResourceRoleEntity> findByUserIdAndResourceTypeAndResourceId(Long userId, ResourceType resourceType, String resourceId);

    List<ResourceRoleEntity> findAllByResourceTypeAndResourceId(ResourceType resourceType, String resourceId);
}
