package com.shipmate.api.callrecording;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CallRecordingRepository {
    private final ConcurrentHashMap<String, CallRecording> recordings = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Path storagePath = Path.of("data", "call-recordings.json");

    public CallRecordingRepository() {
        loadFromDisk();
    }

    public CallRecording save(CallRecording recording) {
        recordings.put(recording.recordingId(), recording);
        persistToDisk();
        return recording;
    }

    public Optional<CallRecording> findById(String recordingId) {
        return Optional.ofNullable(recordings.get(recordingId));
    }

    public void deleteById(String recordingId) {
        recordings.remove(recordingId);
        persistToDisk();
    }

    public List<CallRecording> findRecent(int limit) {
        return recordings.values().stream()
                .sorted(Comparator.comparing(CallRecording::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    public List<CallRecording> findAll() {
        return recordings.values().stream()
                .sorted(Comparator.comparing(CallRecording::createdAt).reversed())
                .toList();
    }

    private void loadFromDisk() {
        if (!Files.exists(storagePath)) {
            return;
        }

        try {
            List<CallRecording> savedRecordings = objectMapper.readValue(
                    storagePath.toFile(),
                    new TypeReference<>() {
                    }
            );
            savedRecordings.forEach(recording -> recordings.put(recording.recordingId(), recording));
        } catch (Exception exception) {
            throw new IllegalStateException("디스크에서 통화 녹음 저장소를 불러오지 못했습니다.", exception);
        }
    }

    private synchronized void persistToDisk() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storagePath.toFile(), recordings.values());
        } catch (Exception exception) {
            throw new IllegalStateException("통화 녹음 저장소를 디스크에 저장하지 못했습니다.", exception);
        }
    }
}
