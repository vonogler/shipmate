package com.shipmate.api.operation.settings;

import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.shippingorder.CourierCsvTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class MerchantOperationSettingsService {
    private final MerchantOperationSettingsRepository repository;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;

    public MerchantOperationSettingsService(
            MerchantOperationSettingsRepository repository,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
    }

    public MerchantOperationSettings save(MerchantOperationSettingsRequest request) {
        String merchantId = requestAuthContext.currentMerchantIdOr(request.merchantId().strip());
        MerchantOperationSettings saved = repository.save(new MerchantOperationSettings(
                merchantId,
                blankToNull(request.defaultMerchantStaffId()),
                request.defaultCourierCsvTemplate() == null ? CourierCsvTemplate.GENERIC : request.defaultCourierCsvTemplate(),
                request.defaultBusinessChannel() == null ? BusinessChannel.PACKAGING_CUSTOM : request.defaultBusinessChannel(),
                OffsetDateTime.now()
        ));
        auditLogService.appendForCurrentActor(
                merchantId,
                null,
                "MERCHANT_OPERATION_SETTINGS_SAVED",
                "MERCHANT_OPERATION_SETTINGS",
                merchantId,
                "파일럿용 초기 설정이 저장되었습니다."
        );
        return saved;
    }

    public MerchantOperationSettings get(String merchantId) {
        String readableMerchantId = requestAuthContext.currentMerchantIdOr(merchantId);
        return repository.findByMerchantId(readableMerchantId)
                .orElse(new MerchantOperationSettings(
                        readableMerchantId,
                        null,
                        CourierCsvTemplate.GENERIC,
                        BusinessChannel.PACKAGING_CUSTOM,
                        null
                ));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
