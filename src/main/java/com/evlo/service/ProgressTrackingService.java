package com.evlo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTrackingService {

    private static final String PROGRESS_KEY_PREFIX = "upload:progress:";
    private static final String META_KEY_PREFIX = "upload:meta:";
    private static final Duration TTL = Duration.ofHours(24); // 24시간

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * 업로드 진행률 저장
     */
    public Mono<Boolean> saveProgress(Long fileId, int currentCount, int totalCount) {
        String key = PROGRESS_KEY_PREFIX + fileId;
        double progress = totalCount > 0 ? (double) currentCount / totalCount * 100 : 0;
        String value = String.format("%.2f", progress);
        
        return redisTemplate.opsForValue()
                .set(key, value, TTL)
                .doOnSuccess(saved -> {
                    if (saved) {
                        log.debug("Progress saved for file {}: {}% ({}/{})", fileId, value, currentCount, totalCount);
                    }
                });
    }

    /**
     * 진행률 조회
     */
    public Mono<Double> getProgress(Long fileId) {
        String key = PROGRESS_KEY_PREFIX + fileId;
        
        return redisTemplate.opsForValue()
                .get(key)
                .map(value -> {
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid progress value for file {}: {}", fileId, value);
                        return 0.0;
                    }
                })
                .defaultIfEmpty(0.0);
    }

    /**
     * 파일 메타정보 저장
     */
    public Mono<Boolean> saveFileMeta(Long fileId, String filename, long fileSize) {
        String key = META_KEY_PREFIX + fileId;
        String value = String.format("%s|%d", filename, fileSize);
        
        return redisTemplate.opsForValue()
                .set(key, value, TTL)
                .doOnSuccess(saved -> {
                    if (saved) {
                        log.debug("Saved file meta to cache: {} -> {}", fileId, filename);
                    }
                });
    }

    /**
     * 파일 메타정보 조회
     */
    public Mono<String> getFileMeta(Long fileId) {
        String key = META_KEY_PREFIX + fileId;
        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty("");
    }

    /**
     * 진행률 삭제
     */
    public Mono<Long> deleteProgress(Long fileId) {
        String progressKey = PROGRESS_KEY_PREFIX + fileId;
        String metaKey = META_KEY_PREFIX + fileId;
        
        return redisTemplate.delete(progressKey)
                .flatMap(count -> redisTemplate.delete(metaKey)
                        .map(count2 -> count + count2));
    }
}
