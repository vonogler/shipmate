package com.shipmate.api.emailorder;

import com.shipmate.api.inboundmessage.CreateInboundMessageRequest;
import com.shipmate.api.inboundmessage.InboundMessageResponse;
import com.shipmate.api.inboundmessage.InboundMessageService;
import com.shipmate.api.inboundmessage.SourceChannel;
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
@RequestMapping("/api/email-orders")
public class EmailOrderController {
    private final InboundMessageService inboundMessageService;
    private final EmailMailboxService emailMailboxService;
    private final EmailAttachmentService emailAttachmentService;

    public EmailOrderController(
            InboundMessageService inboundMessageService,
            EmailMailboxService emailMailboxService,
            EmailAttachmentService emailAttachmentService
    ) {
        this.inboundMessageService = inboundMessageService;
        this.emailMailboxService = emailMailboxService;
        this.emailAttachmentService = emailAttachmentService;
    }

    @PostMapping
    InboundMessageResponse create(@Valid @RequestBody CreateEmailOrderRequest request) {
        String rawText = """
                보낸 사람: %s
                제목: %s

                %s
                """.formatted(
                valueOrBlank(request.fromEmail()),
                valueOrBlank(request.subject()),
                request.body().strip()
        ).strip();

        return inboundMessageService.create(new CreateInboundMessageRequest(
                request.merchantId(),
                request.merchantStaffId(),
                request.customerId(),
                SourceChannel.EMAIL,
                request.businessChannel(),
                rawText,
                null
        ));
    }

    @PostMapping("/mailbox-config")
    EmailMailboxConfigResponse saveMailboxConfig(@Valid @RequestBody EmailMailboxConfigRequest request) {
        return emailMailboxService.saveConfig(request);
    }

    @PostMapping("/collect-mailbox")
    EmailMailboxConfigResponse collectMailbox(@RequestBody java.util.Map<String, String> request) {
        return emailMailboxService.collect(request.get("merchantId"));
    }

    @GetMapping("/attachments")
    List<EmailAttachmentResponse> attachments(@RequestParam String inboundMessageId) {
        return emailAttachmentService.listByInboundMessageId(inboundMessageId);
    }

    @GetMapping("/attachments/{emailAttachmentId}/download-url")
    EmailAttachmentDownloadUrlResponse attachmentDownloadUrl(@PathVariable String emailAttachmentId) {
        return emailAttachmentService.downloadUrl(emailAttachmentId);
    }

    @PostMapping("/attachments/cleanup-expired")
    EmailAttachmentCleanupResponse cleanupExpiredAttachments(@RequestParam(required = false) Integer limit) {
        return emailAttachmentService.cleanupExpiredAttachments(limit);
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
