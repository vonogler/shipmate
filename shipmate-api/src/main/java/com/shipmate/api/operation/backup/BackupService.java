package com.shipmate.api.operation.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.operation.audit.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {
    private final Path dataPath = Path.of("data");
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;

    public BackupService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
    }

    public BackupResponse exportBackup() {
        Map<String, String> files = new LinkedHashMap<>();
        exportDatabaseTables(files);
        auditLogService.appendForCurrentActor(
                requestAuthContext.currentMerchantIdOr("operation"),
                null,
                "BACKUP_EXPORTED",
                "BACKUP",
                null,
                "운영 백업을 내보냈습니다."
        );
        return new BackupResponse(backupId(), files);
    }

    private void exportDatabaseTables(Map<String, String> files) {
        for (String tableName : List.of(
                "tb_code",
                "tb_merchant_users",
                "tb_merchant_admin_profiles",
                "tb_customers",
                "tb_customer_price_items",
                "tb_inbound_messages",
                "tb_order_drafts",
                "tb_order_draft_item_candidates",
                "tb_email_mailbox_configs",
                "tb_audit_logs",
                "tb_error_logs",
                "tb_merchant_operation_settings"
        )) {
            try {
                files.put(tableName + ".db.json", objectMapper.writeValueAsString(
                        jdbcTemplate.queryForList("select * from " + tableName)
                ));
            } catch (Exception exception) {
                files.put(tableName + ".db-error.txt", "DB export 실패: " + exception.getMessage());
            }
        }
    }

    public BackupResponse stageRestore(RestoreBackupRequest request) {
        try {
            Files.createDirectories(dataPath);
            Map<String, String> stagedFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : request.files().entrySet()) {
                String fileName = sanitizeFileName(entry.getKey()) + ".restore.json";
                Path target = dataPath.resolve(fileName);
                Files.writeString(target, entry.getValue());
                stagedFiles.put(fileName, entry.getValue());
            }
            auditLogService.appendForCurrentActor(
                    requestAuthContext.currentMerchantIdOr("operation"),
                    null,
                    "BACKUP_RESTORE_STAGED",
                    "BACKUP",
                    null,
                    "복구 후보 파일이 staging 저장되었습니다."
            );
            return new BackupResponse(backupId(), stagedFiles);
        } catch (Exception exception) {
            throw new IllegalStateException("백업 복구 파일을 저장하지 못했습니다.", exception);
        }
    }

    public ApprovedRestoreResponse approveRestore() {
        if (!requestAuthContext.hasRole("ADMIN")) {
            throw new ApiRequestException("SM-BAC-9001", HttpStatus.FORBIDDEN, "백업 복구 승인은 관리자만 할 수 있습니다.");
        }

        try {
            Files.createDirectories(dataPath);
            String restoreId = backupId();
            List<String> restoredFiles = new ArrayList<>();
            List<String> safetyBackupFiles = new ArrayList<>();
            try (var stream = Files.list(dataPath)) {
                for (Path stagedPath : stream
                        .filter(path -> path.getFileName().toString().endsWith(".restore.json"))
                        .sorted()
                        .toList()) {
                    String stagedName = stagedPath.getFileName().toString();
                    String liveName = stagedName.replaceFirst("\\.restore\\.json$", ".json");
                    Path livePath = dataPath.resolve(liveName);
                    if (Files.exists(livePath)) {
                        String safetyName = liveName.replaceFirst("\\.json$", "." + restoreId + ".before-restore.json");
                        Files.copy(livePath, dataPath.resolve(safetyName));
                        safetyBackupFiles.add(safetyName);
                    }
                    Files.copy(stagedPath, livePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    restoredFiles.add(liveName);
                }
            }
            auditLogService.appendForCurrentActor(
                    requestAuthContext.currentMerchantIdOr("operation"),
                    null,
                    "BACKUP_RESTORE_APPROVED",
                    "BACKUP",
                    restoreId,
                    "staging 복구 파일이 운영 데이터에 반영되었습니다."
            );
            return new ApprovedRestoreResponse(restoreId, restoredFiles, safetyBackupFiles);
        } catch (Exception exception) {
            throw new IllegalStateException("백업 복구 승인 처리를 완료하지 못했습니다.", exception);
        }
    }

    private String backupId() {
        return "bak_" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "-").replaceFirst("\\.json$", "");
    }
}
