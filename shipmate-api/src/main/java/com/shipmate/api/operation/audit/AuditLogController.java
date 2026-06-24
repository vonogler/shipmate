package com.shipmate.api.operation.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operation/audit-logs")
public class AuditLogController {
    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    List<AuditLog> recent(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String metadataKey,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return service.search(new AuditLogSearchCriteria(
                actionType,
                targetType,
                merchantId,
                metadataKey,
                searchText,
                days,
                limit,
                offset
        )).rows();
    }
}
