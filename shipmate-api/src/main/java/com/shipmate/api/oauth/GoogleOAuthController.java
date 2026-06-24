package com.shipmate.api.oauth;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/oauth/google")
public class GoogleOAuthController {
    private final GoogleOAuthService service;
    private final GoogleGmailPollingService pollingService;

    public GoogleOAuthController(GoogleOAuthService service, GoogleGmailPollingService pollingService) {
        this.service = service;
        this.pollingService = pollingService;
    }

    @PostMapping("/authorize-url")
    GoogleOAuthAuthorizeUrlResponse authorizeUrl(@RequestBody(required = false) GoogleOAuthAuthorizeUrlRequest request) {
        return service.createAuthorizeUrl(request);
    }

    @GetMapping("/status")
    GoogleOAuthStatusResponse status(@RequestParam(required = false) String merchantId) {
        return service.status(merchantId);
    }

    @GetMapping("/gmail-polling/status")
    GoogleGmailPollingStatusResponse gmailPollingStatus() {
        return pollingService.status();
    }

    @GetMapping("/gmail-polling/runs")
    List<GoogleGmailPollingRun> gmailPollingRuns(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit
    ) {
        return pollingService.searchRuns(merchantId, success, days, limit);
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        return service.handleCallback(code, state, error);
    }

    @PostMapping("/collect-gmail")
    GoogleGmailPollingResponse collectGmail(@RequestBody(required = false) GoogleOAuthAuthorizeUrlRequest request) {
        if (request == null || request.merchantId() == null || request.merchantId().isBlank()) {
            return pollingService.collectAllConnectedAccounts();
        }
        return pollingService.collectMerchant(request.merchantId());
    }

    @PostMapping("/mark-latest-collected-read")
    GoogleGmailMarkReadResponse markLatestCollectedRead(
            @RequestBody(required = false) GoogleOAuthAuthorizeUrlRequest request,
            @RequestParam(required = false) Integer limit
    ) {
        String merchantId = request == null ? null : request.merchantId();
        return pollingService.markLatestCollectedMessagesRead(merchantId, limit);
    }
}
