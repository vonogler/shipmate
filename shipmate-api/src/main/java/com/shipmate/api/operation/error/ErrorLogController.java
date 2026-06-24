package com.shipmate.api.operation.error;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operation/error-logs")
public class ErrorLogController {
    private final ErrorLogService service;

    public ErrorLogController(ErrorLogService service) {
        this.service = service;
    }

    @GetMapping
    List<ErrorLog> recent(
            @RequestParam(required = false) String metadataKey,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String handlingStatus,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit
    ) {
        return service.searchPage(metadataKey, searchText, handlingStatus, days, limit, 0).rows();
    }
}
