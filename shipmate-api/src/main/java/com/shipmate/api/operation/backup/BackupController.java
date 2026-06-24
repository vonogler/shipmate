package com.shipmate.api.operation.backup;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation/backups")
public class BackupController {
    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @GetMapping("/export")
    BackupResponse exportBackup() {
        return service.exportBackup();
    }

    @PostMapping("/restore-stage")
    BackupResponse stageRestore(@Valid @RequestBody RestoreBackupRequest request) {
        return service.stageRestore(request);
    }

    @PostMapping("/restore-approve")
    ApprovedRestoreResponse approveRestore() {
        return service.approveRestore();
    }
}
