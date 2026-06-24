package com.shipmate.api.operation.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation/dashboard")
public class OperationDashboardController {
    private final OperationDashboardService service;

    public OperationDashboardController(OperationDashboardService service) {
        this.service = service;
    }

    @GetMapping
    OperationDashboardResponse snapshot(@RequestParam(required = false) Integer days) {
        return service.snapshot(days);
    }
}
