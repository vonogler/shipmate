package com.shipmate.api.callrecording;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CallRecordingRepository {
    private final ConcurrentHashMap<String, CallRecording> recordings = new ConcurrentHashMap<>();

    public CallRecording save(CallRecording recording) {
        recordings.put(recording.recordingId(), recording);
        return recording;
    }

    public Optional<CallRecording> findById(String recordingId) {
        return Optional.ofNullable(recordings.get(recordingId));
    }

    public List<CallRecording> findRecent(int limit) {
        return recordings.values().stream()
                .sorted(Comparator.comparing(CallRecording::createdAt).reversed())
                .limit(limit)
                .toList();
    }
}
