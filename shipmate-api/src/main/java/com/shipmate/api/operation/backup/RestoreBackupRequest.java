package com.shipmate.api.operation.backup;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record RestoreBackupRequest(
        @NotEmpty Map<String, String> files
) {
}
