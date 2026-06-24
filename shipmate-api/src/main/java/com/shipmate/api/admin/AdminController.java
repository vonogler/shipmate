package com.shipmate.api.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.auth.AuthService;
import com.shipmate.api.auth.LoginResponse;
import com.shipmate.api.auth.MerchantUser;
import com.shipmate.api.auth.MerchantUserRepository;
import com.shipmate.api.callrecording.CallRecording;
import com.shipmate.api.callrecording.CallRecordingRepository;
import com.shipmate.api.customer.Customer;
import com.shipmate.api.customer.CustomerRepository;
import com.shipmate.api.inboundmessage.InboundMessage;
import com.shipmate.api.inboundmessage.InboundMessageRepository;
import com.shipmate.api.operation.audit.AuditLog;
import com.shipmate.api.operation.audit.AuditLogFilterOptionsResponse;
import com.shipmate.api.operation.audit.AuditLogPageResponse;
import com.shipmate.api.operation.audit.AuditLogSearchCriteria;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.operation.backup.BackupResponse;
import com.shipmate.api.operation.backup.BackupService;
import com.shipmate.api.operation.error.ErrorLog;
import com.shipmate.api.operation.error.ErrorLogPageResponse;
import com.shipmate.api.operation.error.ErrorLogService;
import com.shipmate.api.operation.settings.MerchantOperationSettings;
import com.shipmate.api.operation.settings.MerchantOperationSettingsRepository;
import com.shipmate.api.oauth.GoogleGmailPollingService;
import com.shipmate.api.oauth.GoogleGmailPollingRun;
import com.shipmate.api.oauth.GoogleGmailPollingStatusResponse;
import com.shipmate.api.orderdraft.OrderDraft;
import com.shipmate.api.orderdraft.OrderDraftRepository;
import com.shipmate.api.orderdraft.OrderDraftStatus;
import com.shipmate.api.shippingorder.CourierCsvTemplate;
import com.shipmate.api.shippingorder.CourierCsvValidationRule;
import com.shipmate.api.shippingorder.CourierCsvValidationRuleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuthService authService;
    private final MerchantUserRepository merchantUserRepository;
    private final CustomerRepository customerRepository;
    private final InboundMessageRepository inboundMessageRepository;
    private final OrderDraftRepository orderDraftRepository;
    private final CallRecordingRepository callRecordingRepository;
    private final AuditLogService auditLogService;
    private final ErrorLogService errorLogService;
    private final BackupService backupService;
    private final MerchantOperationSettingsRepository settingsRepository;
    private final AdminMerchantProfileRepository merchantProfileRepository;
    private final GoogleGmailPollingService googleGmailPollingService;
    private final CourierCsvValidationRuleRepository courierCsvValidationRuleRepository;

    public AdminController(
            AuthService authService,
            MerchantUserRepository merchantUserRepository,
            CustomerRepository customerRepository,
            InboundMessageRepository inboundMessageRepository,
            OrderDraftRepository orderDraftRepository,
            CallRecordingRepository callRecordingRepository,
            AuditLogService auditLogService,
            ErrorLogService errorLogService,
            BackupService backupService,
            MerchantOperationSettingsRepository settingsRepository,
            AdminMerchantProfileRepository merchantProfileRepository,
            GoogleGmailPollingService googleGmailPollingService,
            CourierCsvValidationRuleRepository courierCsvValidationRuleRepository
    ) {
        this.authService = authService;
        this.merchantUserRepository = merchantUserRepository;
        this.customerRepository = customerRepository;
        this.inboundMessageRepository = inboundMessageRepository;
        this.orderDraftRepository = orderDraftRepository;
        this.callRecordingRepository = callRecordingRepository;
        this.auditLogService = auditLogService;
        this.errorLogService = errorLogService;
        this.backupService = backupService;
        this.settingsRepository = settingsRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.googleGmailPollingService = googleGmailPollingService;
        this.courierCsvValidationRuleRepository = courierCsvValidationRuleRepository;
    }

    @GetMapping("/overview")
    AdminOverviewResponse overview(@RequestParam(required = false) Integer days) {
        int normalizedDays = normalizeDays(days);
        OffsetDateTime from = OffsetDateTime.now().minusDays(normalizedDays);
        List<InboundMessage> inboundMessages = inboundMessageRepository.findAll().stream()
                .filter(row -> !row.receivedAt().isBefore(from))
                .toList();
        List<OrderDraft> orderDrafts = orderDraftRepository.findAll().stream()
                .filter(row -> !row.createdAt().isBefore(from) || !row.updatedAt().isBefore(from))
                .toList();
        List<OrderDraft> shippingOrders = orderDrafts.stream()
                .filter(orderDraft -> orderDraft.orderDraftStatus() == OrderDraftStatus.SUBMITTED)
                .toList();
        List<ErrorLog> errors = errorLogService.recent(500).stream()
                .filter(row -> !row.createdAt().isBefore(from))
                .toList();
        GoogleGmailPollingStatusResponse gmailStatus = googleGmailPollingService.status();
        AdminSmokeReportResponse smokeReport = readSmokeReport();
        return new AdminOverviewResponse(
                normalizedDays,
                distinctMerchantCount(inboundMessages, orderDrafts, customerRepository.findAll()),
                merchantUserRepository.findRecent(MAX_LIMIT).size(),
                inboundMessages.size(),
                orderDrafts.size(),
                shippingOrders.size(),
                callRecordingRepository.findAll().size(),
                errors.size(),
                inboundMessages.stream().collect(Collectors.groupingBy(row -> row.sourceChannel().name(), Collectors.counting())),
                inboundMessages.stream().collect(Collectors.groupingBy(row -> row.businessChannel().name(), Collectors.counting())),
                orderDrafts.stream().collect(Collectors.groupingBy(row -> row.orderDraftStatus().name(), Collectors.counting())),
                errors.stream().collect(Collectors.groupingBy(ErrorLog::code, Collectors.counting())),
                inboundMessages.stream()
                        .filter(row -> row.inboundMessageStatus().name().equals("FAILED"))
                        .collect(Collectors.groupingBy(row -> row.sourceChannel().name(), Collectors.counting())),
                inboundMessages.stream()
                        .filter(row -> row.inboundMessageStatus().name().equals("FAILED"))
                        .collect(Collectors.groupingBy(
                                row -> row.sourceChannel().name(),
                                Collectors.groupingBy(AdminController::failureReason, Collectors.counting())
                        )),
                errors.stream().collect(Collectors.groupingBy(ErrorLog::handlingStatus, Collectors.counting())),
                java.util.stream.Stream.concat(
                                inboundMessages.stream().map(InboundMessage::merchantId),
                                orderDrafts.stream().map(OrderDraft::merchantId)
                        )
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.groupingBy(value -> value, Collectors.counting())),
                gmailStatus.pollingEnabled(),
                gmailStatus.pollingDelayMs(),
                gmailStatus.accountCount(),
                gmailStatus.recentRunCount(),
                gmailStatus.recentFailureCount(),
                gmailStatus.recentSkippedNonOrderCount(),
                gmailStatus.latestSuccessAt(),
                gmailStatus.latestFailureAt(),
                gmailStatus.latestFailureCode(),
                gmailStatus.latestFailureMessage(),
                gmailStatus.latestFailureInboundMessageId(),
                gmailStatus.recentFailureCodeCounts(),
                smokeReport
        );
    }

    @GetMapping(value = "/staging-summary.html", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> stagingSummaryHtml() throws IOException {
        String configuredPath = System.getenv().getOrDefault(
                "SHIPMATE_STAGING_SUMMARY_HTML_FILE",
                "shipmate-infra/reports/staging-check-summary.html"
        );
        Path summaryPath = Path.of(configuredPath);
        if (!Files.isRegularFile(summaryPath)) {
            return ResponseEntity
                    .status(404)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<!doctype html><html lang=\"ko\"><body><p>스테이징 점검 요약 파일이 없습니다.</p></body></html>");
        }
        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(Files.readString(summaryPath));
    }

    @GetMapping("/merchants")
    List<AdminMerchantResponse> merchants() {
        Map<String, Long> customerCounts = customerRepository.findAll().stream()
                .collect(Collectors.groupingBy(Customer::merchantId, Collectors.counting()));
        Map<String, Long> inboundCounts = inboundMessageRepository.findAll().stream()
                .collect(Collectors.groupingBy(InboundMessage::merchantId, Collectors.counting()));
        Map<String, Long> draftCounts = orderDraftRepository.findAll().stream()
                .collect(Collectors.groupingBy(OrderDraft::merchantId, Collectors.counting()));
        Map<String, OffsetDateTime> recentActivity = orderDraftRepository.findAll().stream()
                .collect(Collectors.toMap(OrderDraft::merchantId, OrderDraft::updatedAt, AdminController::maxTime));
        Map<String, AdminMerchantProfile> profiles = merchantProfileRepository.findAllAsMap();

        return merchantUserRepository.findRecent(MAX_LIMIT).stream()
                .collect(Collectors.groupingBy(MerchantUser::merchantId))
                .entrySet()
                .stream()
                .map(entry -> {
                    AdminMerchantProfile profile = profiles.get(entry.getKey());
                    boolean locked = entry.getValue().stream().allMatch(user -> user.lockedAt() != null);
                    return new AdminMerchantResponse(
                            entry.getKey(),
                            entry.getKey().equals(AuthService.INTERNAL_MERCHANT_ID) ? "출고비서 내부" : entry.getKey(),
                            profile == null ? "ACTIVE" : profile.merchantStatus(),
                            locked,
                            entry.getValue().size(),
                            customerCounts.getOrDefault(entry.getKey(), 0L),
                            inboundCounts.getOrDefault(entry.getKey(), 0L),
                            draftCounts.getOrDefault(entry.getKey(), 0L),
                            recentActivity.get(entry.getKey()),
                            profile == null || profile.operationMemo() == null ? "" : profile.operationMemo()
                    );
                })
                .sorted((left, right) -> left.merchantId().compareTo(right.merchantId()))
                .toList();
    }

    @PutMapping("/merchants/{merchantId}")
    AdminMerchantProfile updateMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody AdminUpdateMerchantRequest request
    ) {
        AdminMerchantProfile saved = merchantProfileRepository.save(new AdminMerchantProfile(
                merchantId,
                normalizeMerchantStatus(request.status()),
                blankToNull(request.operationMemo()),
                OffsetDateTime.now()
        ));
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_MERCHANT_UPDATED",
                "MERCHANT",
                merchantId,
                "이용자/가맹점 운영 정보가 수정되었습니다."
        );
        return saved;
    }

    @PostMapping("/merchants/{merchantId}/lock")
    AdminMerchantAccountActionResponse lockMerchant(@PathVariable String merchantId) {
        int updatedCount = merchantUserRepository.updateLockedAtByMerchantId(merchantId, OffsetDateTime.now(), OffsetDateTime.now());
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_MERCHANT_LOCKED",
                "MERCHANT",
                merchantId,
                "이용자/가맹점 계정이 잠금 처리되었습니다."
        );
        return new AdminMerchantAccountActionResponse(merchantId, true, null, updatedCount);
    }

    @PostMapping("/merchants/{merchantId}/unlock")
    AdminMerchantAccountActionResponse unlockMerchant(@PathVariable String merchantId) {
        int updatedCount = merchantUserRepository.updateLockedAtByMerchantId(merchantId, null, OffsetDateTime.now());
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_MERCHANT_UNLOCKED",
                "MERCHANT",
                merchantId,
                "이용자/가맹점 계정 잠금이 해제되었습니다."
        );
        return new AdminMerchantAccountActionResponse(merchantId, false, null, updatedCount);
    }

    @PostMapping("/merchants/{merchantId}/reset-password")
    AdminMerchantAccountActionResponse resetMerchantPassword(@PathVariable String merchantId) {
        String temporaryPassword = temporaryPassword();
        int updatedCount = merchantUserRepository.updatePasswordByMerchantId(
                merchantId,
                authService.passwordHash(temporaryPassword),
                OffsetDateTime.now()
        );
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_MERCHANT_PASSWORD_RESET",
                "MERCHANT",
                merchantId,
                "이용자/가맹점 계정 비밀번호가 초기화되었습니다."
        );
        return new AdminMerchantAccountActionResponse(merchantId, false, temporaryPassword, updatedCount);
    }

    @GetMapping("/users")
    List<AdminUserResponse> users(@RequestParam(required = false) Integer limit) {
        return merchantUserRepository.findRecent(limit(limit)).stream()
                .filter(user -> AuthService.INTERNAL_MERCHANT_ID.equals(user.merchantId()))
                .map(AdminUserResponse::from)
                .toList();
    }

    @PostMapping("/users")
    LoginResponse createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return authService.registerInternal(
                request.loginId(),
                request.password(),
                request.displayName(),
                request.role()
        );
    }

    @GetMapping("/inbound-messages")
    List<InboundMessage> inboundMessages(@RequestParam(required = false) Integer limit) {
        return inboundMessageRepository.findAll().stream().limit(limit(limit)).toList();
    }

    @GetMapping("/order-drafts")
    List<OrderDraft> orderDrafts(@RequestParam(required = false) Integer limit) {
        return orderDraftRepository.findAll().stream()
                .filter(orderDraft -> orderDraft.orderDraftStatus() != OrderDraftStatus.SUBMITTED)
                .limit(limit(limit))
                .toList();
    }

    @GetMapping("/shipping-orders")
    List<OrderDraft> shippingOrders(@RequestParam(required = false) Integer limit) {
        return orderDraftRepository.findAll().stream()
                .filter(orderDraft -> orderDraft.orderDraftStatus() == OrderDraftStatus.SUBMITTED)
                .limit(limit(limit))
                .toList();
    }

    @GetMapping("/call-recordings")
    List<CallRecording> callRecordings(@RequestParam(required = false) Integer limit) {
        return callRecordingRepository.findAll().stream().limit(limit(limit)).toList();
    }

    @GetMapping("/audit-logs")
    AuditLogPageResponse auditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String metadataKey,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return auditLogService.search(new AuditLogSearchCriteria(
                actionType,
                targetType,
                merchantId,
                metadataKey,
                searchText,
                days,
                limit,
                offset
        ));
    }

    @GetMapping("/audit-logs/filter-options")
    AuditLogFilterOptionsResponse auditLogFilterOptions() {
        return auditLogService.filterOptions();
    }

    @GetMapping("/error-logs")
    ErrorLogPageResponse errorLogs(
            @RequestParam(required = false) String metadataKey,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) String handlingStatus,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return errorLogService.searchPage(metadataKey, searchText, handlingStatus, days, limit(limit), offset);
    }

    @GetMapping("/gmail-polling-runs")
    List<GoogleGmailPollingRun> gmailPollingRuns(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit
    ) {
        return googleGmailPollingService.searchRuns(merchantId, success, days, limit(limit));
    }

    @PutMapping("/error-logs/{errorLogId}/handling")
    ErrorLog updateErrorHandling(
            @PathVariable String errorLogId,
            @Valid @RequestBody AdminUpdateErrorHandlingRequest request
    ) {
        ErrorLog updated = errorLogService.updateHandling(
                errorLogId,
                request.handlingStatus(),
                request.assignedActorId(),
                request.handlingMemo()
        );
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_ERROR_HANDLING_UPDATED",
                "ERROR_LOG",
                errorLogId,
                "오류 로그 처리 상태가 수정되었습니다."
        );
        return updated;
    }

    @PutMapping("/error-logs/handling")
    List<ErrorLog> updateErrorHandlingBulk(@Valid @RequestBody AdminBulkUpdateErrorHandlingRequest request) {
        return request.errorLogIds().stream()
                .map(errorLogId -> {
                    ErrorLog updated = errorLogService.updateHandling(
                            errorLogId,
                            request.handlingStatus(),
                            request.assignedActorId(),
                            request.handlingMemo()
                    );
                    auditLogService.append(
                            AuthService.INTERNAL_MERCHANT_ID,
                            null,
                            "ADMIN_ERROR_HANDLING_UPDATED",
                            "ERROR_LOG",
                            errorLogId,
                            "오류 로그 처리 상태가 일괄 수정되었습니다."
                    );
                    return updated;
                })
                .toList();
    }

    @GetMapping("/settings")
    List<MerchantOperationSettings> settings() {
        return settingsRepository.findAll();
    }

    @GetMapping("/settings/courier-csv-validation-rules")
    List<CourierCsvValidationRule> courierCsvValidationRules() {
        return courierCsvValidationRuleRepository.findAll();
    }

    @PutMapping("/settings/courier-csv-validation-rules/{ruleId}")
    CourierCsvValidationRule updateCourierCsvValidationRule(
            @PathVariable String ruleId,
            @Valid @RequestBody AdminUpdateCourierCsvValidationRuleRequest request
    ) {
        CourierCsvValidationRule current = courierCsvValidationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new com.shipmate.api.common.ApiRequestException("SM-SHP-6003", org.springframework.http.HttpStatus.NOT_FOUND, "택배 CSV 검증 규칙을 찾을 수 없습니다."));
        CourierCsvValidationRule saved = courierCsvValidationRuleRepository.save(new CourierCsvValidationRule(
                current.courierCsvValidationRuleId(),
                current.courierCsvTemplate(),
                current.fieldName(),
                blankToNull(request.csvColumnName()),
                request.required(),
                request.errorMessage().strip(),
                request.sortOrder(),
                request.active()
        ));
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_COURIER_CSV_RULE_UPDATED",
                "COURIER_CSV_VALIDATION_RULE",
                ruleId,
                "택배 CSV 검증 규칙이 수정되었습니다.",
                courierCsvRuleMetadata(current, saved)
        );
        return saved;
    }

    @PostMapping("/settings/courier-csv-validation-rules")
    CourierCsvValidationRule createCourierCsvValidationRule(@Valid @RequestBody AdminCreateCourierCsvValidationRuleRequest request) {
        CourierCsvTemplate template = CourierCsvTemplate.valueOf(request.courierCsvTemplate().strip());
        String fieldName = request.fieldName().strip();
        CourierCsvValidationRule saved = courierCsvValidationRuleRepository.save(new CourierCsvValidationRule(
                "ccvr_" + UUID.randomUUID().toString().replace("-", ""),
                template,
                fieldName,
                blankToNull(request.csvColumnName()),
                request.required(),
                request.errorMessage().strip(),
                request.sortOrder(),
                request.active()
        ));
        auditLogService.append(
                AuthService.INTERNAL_MERCHANT_ID,
                null,
                "ADMIN_COURIER_CSV_RULE_CREATED",
                "COURIER_CSV_VALIDATION_RULE",
                saved.courierCsvValidationRuleId(),
                "택배 CSV 검증 규칙이 추가되었습니다.",
                courierCsvRuleMetadata(null, saved)
        );
        return saved;
    }

    @GetMapping("/backup")
    BackupResponse backup() {
        return backupService.exportBackup();
    }

    private static int limit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return 7;
        }
        return Math.min(days, 90);
    }

    private static OffsetDateTime maxTime(OffsetDateTime left, OffsetDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private static long distinctMerchantCount(
            List<InboundMessage> inboundMessages,
            List<OrderDraft> orderDrafts,
            List<Customer> customers
    ) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                inboundMessages.stream().map(InboundMessage::merchantId),
                                orderDrafts.stream().map(OrderDraft::merchantId)
                        ),
                        customers.stream().map(Customer::merchantId)
                )
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
    }

    private static String failureReason(InboundMessage inboundMessage) {
        String rawText = inboundMessage.rawText() == null ? "" : inboundMessage.rawText();
        for (String marker : List.of("실패 사유:", "실패사유:", "failureReason:", "failure_reason:")) {
            int index = rawText.indexOf(marker);
            if (index >= 0) {
                String reason = rawText.substring(index + marker.length()).lines().findFirst().orElse("").strip();
                if (!reason.isBlank()) {
                    return reason.length() > 80 ? reason.substring(0, 80) : reason;
                }
            }
        }
        return "사유 미기록";
    }

    private static AdminSmokeReportResponse readSmokeReport() {
        String configuredPath = System.getenv().getOrDefault("SHIPMATE_SMOKE_REPORT_FILE", "shipmate-infra/smoke-report.json");
        Path reportPath = Path.of(configuredPath);
        if (!Files.isRegularFile(reportPath)) {
            return new AdminSmokeReportResponse("none", null, null, null, null, null, null, null, null, reportPath.toString());
        }
        try {
            AdminSmokeReportFile report = OBJECT_MAPPER.readValue(reportPath.toFile(), AdminSmokeReportFile.class);
            return new AdminSmokeReportResponse(
                    blankToDefault(report.status(), "unknown"),
                    report.apiBaseUrl(),
                    report.merchantId(),
                    report.inboundMessageId(),
                    report.orderDraftId(),
                    report.lastStep(),
                    report.lastHttpStatus(),
                    report.failureDetail(),
                    report.finishedAt(),
                    reportPath.toString()
            );
        } catch (IOException exception) {
            return new AdminSmokeReportResponse("unreadable", null, null, null, null, null, exception.getMessage(), null, null, reportPath.toString());
        }
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record AdminOverviewResponse(
            int days,
            long merchantCount,
            long userCount,
            long inboundMessageCount,
            long orderDraftCount,
            long submittedShippingOrderCount,
            long callRecordingCount,
            long errorLogCount,
            Map<String, Long> sourceChannelCounts,
            Map<String, Long> businessChannelCounts,
            Map<String, Long> orderDraftStatusCounts,
            Map<String, Long> errorCodeCounts,
            Map<String, Long> failedSourceChannelCounts,
            Map<String, Map<String, Long>> failedReasonBySourceChannelCounts,
            Map<String, Long> errorHandlingStatusCounts,
            Map<String, Long> merchantActivityCounts,
            boolean gmailPollingEnabled,
            long gmailPollingDelayMs,
            int gmailPollingAccountCount,
            int gmailPollingRecentRunCount,
            int gmailPollingRecentFailureCount,
            int gmailPollingRecentSkippedNonOrderCount,
            OffsetDateTime gmailPollingLatestSuccessAt,
            OffsetDateTime gmailPollingLatestFailureAt,
            String gmailPollingLatestFailureCode,
            String gmailPollingLatestFailureMessage,
            String gmailPollingLatestFailureInboundMessageId,
            Map<String, Long> gmailPollingFailureCodeCounts,
            AdminSmokeReportResponse smokeReport
    ) {
    }

    public record AdminSmokeReportResponse(
            String status,
            String apiBaseUrl,
            String merchantId,
            String inboundMessageId,
            String orderDraftId,
            String lastStep,
            String lastHttpStatus,
            String failureDetail,
            String finishedAt,
            String reportPath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdminSmokeReportFile(
            String status,
            String apiBaseUrl,
            String merchantId,
            String inboundMessageId,
            String orderDraftId,
            String lastStep,
            String lastHttpStatus,
            String failureDetail,
            String finishedAt
    ) {
    }

    public record AdminMerchantResponse(
            String merchantId,
            String merchantName,
            String status,
            boolean accountLocked,
            long userCount,
            long customerCount,
            long inboundMessageCount,
            long orderDraftCount,
            OffsetDateTime lastActivityAt,
            String operationMemo
    ) {
    }

    public record AdminUserResponse(
            String merchantUserId,
            String merchantId,
            String loginId,
            String displayName,
            String role,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        static AdminUserResponse from(MerchantUser user) {
            return new AdminUserResponse(
                    user.merchantUserId(),
                    user.merchantId(),
                    user.loginId(),
                    user.displayName(),
                    user.role(),
                    user.createdAt(),
                    user.updatedAt()
            );
        }
    }

    public record AdminUpdateMerchantRequest(
            @NotBlank @Size(max = 40) String status,
            @Size(max = 1000) String operationMemo
    ) {
    }

    public record AdminMerchantAccountActionResponse(
            String merchantId,
            boolean accountLocked,
            String temporaryPassword,
            int updatedUserCount
    ) {
    }

    public record AdminUpdateErrorHandlingRequest(
            @NotBlank @Size(max = 40) String handlingStatus,
            @Size(max = 80) String assignedActorId,
            @Size(max = 1000) String handlingMemo
    ) {
    }

    public record AdminBulkUpdateErrorHandlingRequest(
            @Size(min = 1, max = 200) List<@NotBlank @Size(max = 80) String> errorLogIds,
            @NotBlank @Size(max = 40) String handlingStatus,
            @Size(max = 80) String assignedActorId,
            @Size(max = 1000) String handlingMemo
    ) {
    }

    public record AdminUpdateCourierCsvValidationRuleRequest(
            @Size(max = 120) String csvColumnName,
            boolean required,
            @NotBlank @Size(max = 500) String errorMessage,
            int sortOrder,
            boolean active
    ) {
    }

    public record AdminCreateCourierCsvValidationRuleRequest(
            @NotBlank @Size(max = 40) String courierCsvTemplate,
            @NotBlank @Size(max = 120) String fieldName,
            @Size(max = 120) String csvColumnName,
            boolean required,
            @NotBlank @Size(max = 500) String errorMessage,
            int sortOrder,
            boolean active
    ) {
    }

    public record AdminCreateUserRequest(
            String merchantId,
            @NotBlank @Size(max = 100) String loginId,
            @NotBlank @Size(min = 4, max = 100) String password,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(max = 50) String role
    ) {
    }

    private static String normalizeMerchantStatus(String status) {
        String normalized = status == null ? "" : status.strip();
        if (normalized.equals("ACTIVE")
                || normalized.equals("PAUSED")
                || normalized.equals("TERMINATED")
                || normalized.equals("REVIEWING")) {
            return normalized;
        }
        return "ACTIVE";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String courierCsvRuleMetadata(CourierCsvValidationRule before, CourierCsvValidationRule after) {
        return """
                {"before":%s,"after":%s}
                """.formatted(courierCsvRuleJson(before), courierCsvRuleJson(after)).strip();
    }

    private static String courierCsvRuleJson(CourierCsvValidationRule rule) {
        if (rule == null) {
            return "null";
        }
        return """
                {"courierCsvTemplate":"%s","fieldName":"%s","csvColumnName":%s,"required":%s,"errorMessage":"%s","sortOrder":%d,"active":%s}
                """.formatted(
                jsonEscape(rule.courierCsvTemplate().name()),
                jsonEscape(rule.fieldName()),
                rule.csvColumnName() == null ? "null" : "\"" + jsonEscape(rule.csvColumnName()) + "\"",
                rule.required(),
                jsonEscape(rule.errorMessage()),
                rule.sortOrder(),
                rule.active()
        ).strip();
    }

    private static String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String temporaryPassword() {
        return "SM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
