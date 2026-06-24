package com.shipmate.api.operation.backup;

import java.util.Map;

public record BackupResponse(
        String backupId,
        Map<String, String> files
) {
}
