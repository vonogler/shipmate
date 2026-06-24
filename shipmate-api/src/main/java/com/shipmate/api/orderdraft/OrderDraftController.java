package com.shipmate.api.orderdraft;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order-drafts")
public class OrderDraftController {
    private final OrderDraftService service;

    public OrderDraftController(OrderDraftService service) {
        this.service = service;
    }

    @PostMapping("/from-inbound-message")
    OrderDraftResponse createFromInboundMessage(@Valid @RequestBody CreateOrderDraftFromInboundMessageRequest request) {
        return service.createFromInboundMessage(request);
    }

    @GetMapping
    List<OrderDraftResponse> recent(@RequestParam(required = false) Integer limit) {
        return service.recent(limit);
    }

    @GetMapping("/{orderDraftId}")
    OrderDraftResponse get(@PathVariable String orderDraftId) {
        return service.get(orderDraftId);
    }

    @PutMapping("/{orderDraftId}/review")
    OrderDraftResponse updateReview(
            @PathVariable String orderDraftId,
            @Valid @RequestBody UpdateOrderDraftReviewRequest request
    ) {
        return service.updateReview(orderDraftId, request);
    }

    @PostMapping("/{orderDraftId}/submit")
    OrderDraftResponse submit(@PathVariable String orderDraftId) {
        return service.submit(orderDraftId);
    }

    @DeleteMapping("/{orderDraftId}")
    OrderDraftResponse cancel(@PathVariable String orderDraftId) {
        return service.cancel(orderDraftId);
    }
}
