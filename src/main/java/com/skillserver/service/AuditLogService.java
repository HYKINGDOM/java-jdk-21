package com.skillserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillserver.domain.entity.AuditLogEntity;
import com.skillserver.domain.enums.ActionType;
import com.skillserver.repository.AuditLogRepository;
import com.skillserver.security.AppUserPrincipal;
import com.skillserver.web.TraceIdFilter;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void success(AppUserPrincipal actor,
                        String action,
                        ActionType actionType,
                        String targetType,
                        String targetId,
                        String targetName,
                        Object detail,
                        long durationMs) {
        save(actor, action, actionType, targetType, targetId, targetName, detail, "success", null, durationMs);
    }

    public void failure(AppUserPrincipal actor,
                        String action,
                        ActionType actionType,
                        String targetType,
                        String targetId,
                        String targetName,
                        Object detail,
                        Throwable throwable,
                        long durationMs) {
        save(actor, action, actionType, targetType, targetId, targetName, detail, "failure",
            throwable == null ? null : throwable.getMessage(), durationMs);
    }

    private void save(AppUserPrincipal actor,
                      String action,
                      ActionType actionType,
                      String targetType,
                      String targetId,
                      String targetName,
                      Object detail,
                      String result,
                      String errorMessage,
                      long durationMs) {
        auditLogRepository.save(AuditLogEntity.builder()
            .traceId(MDC.get(TraceIdFilter.TRACE_ID))
            .timestamp(LocalDateTime.now())
            .actorId(actor == null ? null : actor.getUserId())
            .actorName(actor == null ? "system" : actor.getUsername())
            .action(action)
            .actionType(actionType)
            .targetType(targetType)
            .targetId(targetId)
            .targetName(targetName)
            .detailJson(toJson(detail))
            .result(result)
            .errorMessage(errorMessage)
            .durationMs(durationMs)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getMessage() + "\"}";
        }
    }
}
