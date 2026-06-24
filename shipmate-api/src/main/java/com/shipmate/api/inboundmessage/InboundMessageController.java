package com.shipmate.api.inboundmessage;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inbound-messages")
public class InboundMessageController {
    private final InboundMessageService service;

    public InboundMessageController(InboundMessageService service) {
        this.service = service;
    }

    @PostMapping
    InboundMessageResponse create(@Valid @RequestBody CreateInboundMessageRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<InboundMessageResponse> recent(@RequestParam(required = false) Integer limit) {
        return service.recent(limit);
    }

    @GetMapping("/{inboundMessageId}")
    InboundMessageResponse get(@PathVariable String inboundMessageId) {
        return service.get(inboundMessageId);
    }
}
