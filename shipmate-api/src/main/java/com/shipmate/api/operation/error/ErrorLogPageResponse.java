package com.shipmate.api.operation.error;

import java.util.List;

public record ErrorLogPageResponse(
        List<ErrorLog> rows,
        long total,
        int limit,
        int offset
) {
}
