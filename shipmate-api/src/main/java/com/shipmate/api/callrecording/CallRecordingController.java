package com.shipmate.api.callrecording;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/call-recordings")
public class CallRecordingController {
    private final CallRecordingService service;

    public CallRecordingController(CallRecordingService service) {
        this.service = service;
    }

    @PostMapping("/upload-sessions")
    UploadSessionResponse createUploadSession(@Valid @RequestBody CreateUploadSessionRequest request) {
        return service.createUploadSession(request);
    }

    @PostMapping("/{recordingId}/complete")
    CompleteUploadResponse completeUpload(@PathVariable String recordingId) {
        return service.completeUpload(recordingId);
    }

    @GetMapping("/recent")
    List<RecentCallRecordingResponse> recent() {
        return service.recent();
    }
}
