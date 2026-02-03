package com.evlo.service;

import com.evlo.dto.EventSearchRequest;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String SEARCH_CACHE_PREFIX = "search:";
    private static final String FILE_META_PREFIX = "file:meta:";
    private static final Duration CACHE_TTL = Duration.ofHours(1); // 1시간

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 검색 조건을 캐시 키로 변환 (sessionId 있으면 세션별 캐시)
     */
    private String generateCacheKey(EventSearchRequest searchRequest, Pageable pageable, String sessionId) {
        StringBuilder keyBuilder = new StringBuilder(SEARCH_CACHE_PREFIX);
        if (sessionId != null && !sessionId.isBlank()) {
            keyBuilder.append("session:").append(sessionId).append(":");
        }
        keyBuilder.append("page:").append(pageable.getPageNumber());
        keyBuilder.append(":size:").append(pageable.getPageSize());
        keyBuilder.append(":sort:").append(pageable.getSort());

        if (searchRequest.getStartTime() != null) {
            keyBuilder.append(":start:").append(searchRequest.getStartTime());
        }
        if (searchRequest.getEndTime() != null) {
            keyBuilder.append(":end:").append(searchRequest.getEndTime());
        }
        if (searchRequest.getLevels() != null && !searchRequest.getLevels().isEmpty()) {
            keyBuilder.append(":levels:").append(searchRequest.getLevels().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(",")));
        }
        if (searchRequest.getChannels() != null && !searchRequest.getChannels().isEmpty()) {
            keyBuilder.append(":channels:").append(searchRequest.getChannels().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(",")));
        }
        if (searchRequest.getEventIds() != null && !searchRequest.getEventIds().isEmpty()) {
            keyBuilder.append(":eventIds:").append(searchRequest.getEventIds().stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isEmpty()) {
            keyBuilder.append(":keyword:").append(searchRequest.getKeyword().toLowerCase());
        }
        if (searchRequest.getLogFileId() != null) {
            keyBuilder.append(":logFileId:").append(searchRequest.getLogFileId());
        }
        
        return keyBuilder.toString();
    }

    /**
     * 검색 결과 캐시 저장 (sessionId 있으면 세션별 키 사용)
     */
    public Mono<Boolean> cacheSearchResult(EventSearchRequest searchRequest, Pageable pageable, Page<Event> result, String sessionId) {
        String cacheKey = generateCacheKey(searchRequest, pageable, sessionId);
        
        try {
            String jsonValue = objectMapper.writeValueAsString(result);
            return redisTemplate.opsForValue()
                    .set(cacheKey, jsonValue, CACHE_TTL)
                    .doOnSuccess(saved -> {
                        if (saved) {
                            log.debug("Cached search result: {}", cacheKey);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Error serializing search result for cache: {}", e.getMessage(), e);
            return Mono.just(false);
        }
    }

    /**
     * 검색 결과 캐시 조회
     */
    public Mono<Page<Event>> getCachedSearchResult(EventSearchRequest searchRequest, Pageable pageable, String sessionId) {
        String cacheKey = generateCacheKey(searchRequest, pageable, sessionId);
        
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .flatMap(jsonValue -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Page<Event> result = objectMapper.readValue(jsonValue, Page.class);
                        log.debug("Retrieved cached search result: {}", cacheKey);
                        return Mono.just(result);
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing cached search result: {}", e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(Mono.empty());
    }

    /**
     * 자주 사용되는 검색 조건 캐시 (간단한 통계)
     */
    public Mono<Boolean> incrementSearchCount(EventSearchRequest searchRequest) {
        // 검색 조건의 간단한 해시를 키로 사용
        String conditionKey = generateSimpleConditionKey(searchRequest);
        String countKey = "search:count:" + conditionKey;
        
        return redisTemplate.opsForValue()
                .increment(countKey)
                .then(redisTemplate.expire(countKey, Duration.ofDays(7)))
                .map(expired -> {
                    log.debug("Incremented search count for condition: {}", conditionKey);
                    return expired;
                });
    }

    /**
     * 간단한 검색 조건 키 생성 (통계용)
     */
    private String generateSimpleConditionKey(EventSearchRequest searchRequest) {
        StringBuilder keyBuilder = new StringBuilder();
        
        if (searchRequest.getLevels() != null && !searchRequest.getLevels().isEmpty()) {
            keyBuilder.append("levels:").append(searchRequest.getLevels().size());
        }
        if (searchRequest.getChannels() != null && !searchRequest.getChannels().isEmpty()) {
            keyBuilder.append(":channels:").append(searchRequest.getChannels().size());
        }
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isEmpty()) {
            keyBuilder.append(":hasKeyword");
        }
        
        return keyBuilder.length() > 0 ? keyBuilder.toString() : "all";
    }

    /**
     * 캐시 삭제 (특정 조건)
     */
    public Mono<Long> invalidateSearchCache(String pattern) {
        return redisTemplate.keys(SEARCH_CACHE_PREFIX + pattern)
                .flatMap(key -> redisTemplate.delete(key))
                .collectList()
                .map(keys -> (long) keys.size())
                .doOnSuccess(count -> log.debug("Invalidated {} cache entries with pattern: {}", count, pattern));
    }

    /**
     * 모든 검색 캐시 삭제
     */
    public Mono<Long> clearAllSearchCache() {
        return invalidateSearchCache("*");
    }
}
