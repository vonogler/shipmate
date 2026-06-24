package com.shipmate.api.operation.backup;

import java.util.List;

public record ApprovedRestoreResponse(
        String restoreId,
        List<String> restoredFiles,
        List<String> safetyBackupFiles
) {
}
