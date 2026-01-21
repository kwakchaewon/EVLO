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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final EventRepository eventRepository;

    /**
     * Event ID별 발생 빈도 통계
     */
    @GetMapping("/event-frequency")
    public ResponseEntity<List<EventFrequencyResponse>> getEventFrequency(
            @RequestParam(required = false) Integer limit) {
        
        List<Object[]> results = eventRepository.findEventIdFrequency();
        
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
     * Error/Critical Top N 조회
     */
    @GetMapping("/errors-top")
    public ResponseEntity<List<Event>> getTopErrorsAndCritical(
            @RequestParam(defaultValue = "10") int n) {
        
        Pageable pageable = PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "timeCreated"));
        Page<Event> eventPage = eventRepository.findTopErrorsAndCritical(pageable);
        
        return ResponseEntity.ok(eventPage.getContent());
    }

    /**
     * 시간대별 집중 발생 이벤트 분석
     */
    @GetMapping("/time-based")
    public ResponseEntity<List<TimeBasedAnalysisResponse>> getTimeBasedAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long eventId) {
        
        List<Object[]> results;
        if (eventId != null) {
            // 특정 Event ID의 시간대별 발생 빈도
            results = eventRepository.findEventCountByHourForEventId(eventId, startTime, endTime);
        } else {
            // 전체 이벤트의 시간대별 발생 빈도
            results = eventRepository.findEventCountByHour(startTime, endTime);
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
     * 특정 Event ID의 시간대별 발생 빈도
     */
    @GetMapping("/time-based/{eventId}")
    public ResponseEntity<List<TimeBasedAnalysisResponse>> getTimeBasedAnalysisByEventId(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<Object[]> results = eventRepository.findEventCountByHourForEventId(eventId, startTime, endTime);
        
        List<TimeBasedAnalysisResponse> response = results.stream()
                .map(result -> TimeBasedAnalysisResponse.builder()
                        .timeSlot((String) result[0])
                        .count(((Number) result[1]).longValue())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}
