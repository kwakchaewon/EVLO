package com.evlo.controller;

import com.evlo.dto.AiSummaryRequest;
import com.evlo.dto.AiSummaryResponse;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.repository.EventRepository;
import com.evlo.service.AiSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSummaryController {

    private final AiSummaryService aiSummaryService;
    private final EventRepository eventRepository;

    /**
     * 전체 로그 요약
     */
    @PostMapping("/summarize")
    public ResponseEntity<AiSummaryResponse> summarizeEvents(
            @RequestBody(required = false) AiSummaryRequest request) {
        
        List<Event> events;
        if (request != null && request.getEventIds() != null && !request.getEventIds().isEmpty()) {
            // 특정 Event ID 목록으로 필터링
            Pageable pageable = PageRequest.of(0, 100);
            Page<Event> eventPage = eventRepository.findByEventIdIn(request.getEventIds(), pageable);
            events = eventPage.getContent();
        } else {
            // 최근 100개 이벤트
            Pageable pageable = PageRequest.of(0, 100);
            Page<Event> eventPage = eventRepository.findAll(pageable);
            events = eventPage.getContent();
        }

        AiSummaryResponse response = aiSummaryService.summarizeEvents(events);
        return ResponseEntity.ok(response);
    }

    /**
     * Error/Critical 이벤트 중심 요약
     */
    @PostMapping("/summarize-errors")
    public ResponseEntity<AiSummaryResponse> summarizeErrors() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<Event> errorPage = eventRepository.findTopErrorsAndCritical(pageable);
        List<Event> errorEvents = errorPage.getContent();

        AiSummaryResponse response = aiSummaryService.summarizeErrors(errorEvents);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 Event ID 설명 생성 (초보자용)
     */
    @GetMapping("/explain/{eventId}")
    public ResponseEntity<AiSummaryResponse> explainEventId(@PathVariable Long eventId) {
        AiSummaryResponse response = aiSummaryService.explainEventId(eventId);
        return ResponseEntity.ok(response);
    }
}
