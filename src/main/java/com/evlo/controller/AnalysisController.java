package com.evlo.controller;

import com.evlo.dto.EventFrequencyResponse;
import com.evlo.dto.TimeBasedAnalysisResponse;
import com.evlo.entity.Event;
import com.evlo.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private static final String SESSION_COOKIE_NAME = "EVLO_SESSION";

    private final EventRepository eventRepository;

    private static String getSessionId(ServerWebExchange exchange) {
        var cookie = exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME);
        return cookie != null && cookie.getValue() != null ? cookie.getValue() : null;
    }

    /**
     * Event ID별 발생 빈도 통계 (비회원: 현재 세션 로그만)
     */
    @GetMapping("/event-frequency")
    public ResponseEntity<List<EventFrequencyResponse>> getEventFrequency(
            ServerWebExchange exchange,
            @RequestParam(required = false) Integer limit) {

        String sessionId = getSessionId(exchange);
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Object[]> results = eventRepository.findEventIdFrequencyBySessionId(sessionId);

        List<EventFrequencyResponse> response = results.stream()
                .limit(limit != null ? limit : 100)
                .map(result -> EventFrequencyResponse.builder()
                        .eventId(((Number) result[0]).longValue())
                        .count(((Number) result[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Error/Critical Top N 조회 (비회원: 현재 세션 로그만)
     */
    @GetMapping("/errors-top")
    public ResponseEntity<List<Event>> getTopErrorsAndCritical(
            ServerWebExchange exchange,
            @RequestParam(defaultValue = "10") int n) {

        String sessionId = getSessionId(exchange);
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Pageable pageable = PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "timeCreated"));
        Page<Event> eventPage = eventRepository.findTopErrorsAndCriticalBySessionId(sessionId, pageable);

        return ResponseEntity.ok(eventPage.getContent());
    }

    /**
     * 시간대별 집중 발생 이벤트 분석 (비회원: 현재 세션 로그만)
     */
    @GetMapping("/time-based")
    public ResponseEntity<List<TimeBasedAnalysisResponse>> getTimeBasedAnalysis(
            ServerWebExchange exchange,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long eventId) {

        String sessionId = getSessionId(exchange);
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Object[]> results;
        if (eventId != null) {
            results = eventRepository.findEventCountByHourForEventIdBySessionId(sessionId, eventId, startTime, endTime);
        } else {
            results = eventRepository.findEventCountByHourBySessionId(sessionId, startTime, endTime);
        }

        List<TimeBasedAnalysisResponse> response = results.stream()
                .map(result -> TimeBasedAnalysisResponse.builder()
                        .timeSlot((String) result[0])
                        .count(((Number) result[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 Event ID의 시간대별 발생 빈도 (비회원: 현재 세션 로그만)
     */
    @GetMapping("/time-based/{eventId}")
    public ResponseEntity<List<TimeBasedAnalysisResponse>> getTimeBasedAnalysisByEventId(
            ServerWebExchange exchange,
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        String sessionId = getSessionId(exchange);
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Object[]> results = eventRepository.findEventCountByHourForEventIdBySessionId(sessionId, eventId, startTime, endTime);

        List<TimeBasedAnalysisResponse> response = results.stream()
                .map(result -> TimeBasedAnalysisResponse.builder()
                        .timeSlot((String) result[0])
                        .count(((Number) result[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
