package com.shipmate.api.aiextraction;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-drafts")
public class AiExtractionController {
    private final AiExtractionService service;

    public AiExtractionController(AiExtractionService service) {
        this.service = service;
    }

    @PostMapping("/{orderDraftId}/ai-extract")
    AiExtractionResponse extract(@PathVariable String orderDraftId) {
        return service.extract(orderDraftId);
    }
}
