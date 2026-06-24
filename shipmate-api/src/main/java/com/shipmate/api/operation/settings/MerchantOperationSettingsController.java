package com.shipmate.api.operation.settings;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation/settings")
public class MerchantOperationSettingsController {
    private final MerchantOperationSettingsService service;

    public MerchantOperationSettingsController(MerchantOperationSettingsService service) {
        this.service = service;
    }

    @PostMapping
    MerchantOperationSettings save(@Valid @RequestBody MerchantOperationSettingsRequest request) {
        return service.save(request);
    }

    @GetMapping
    MerchantOperationSettings get(@RequestParam String merchantId) {
        return service.get(merchantId);
    }
}
