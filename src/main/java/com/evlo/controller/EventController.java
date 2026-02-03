package com.evlo.controller;

import com.evlo.dto.EventSearchRequest;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.evlo.repository.EventRepository;
import com.evlo.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final CacheService cacheService;

    /**
     * 홈 페이지
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "EVLO - Windows 이벤트 로그 분석");
        model.addAttribute("contentTemplate", "index");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("showEventsNav", false);
        return "layout/base";
    }

    /**
     * 로그 업로드 페이지
     */
    private static final String SESSION_COOKIE_NAME = "EVLO_SESSION";

    @GetMapping("/upload")
    public String uploadPage(ServerWebExchange exchange, Model model) {
        // 업로드 페이지 방문 시 새 세션 부여 → "새로 고침 시 clear" (이전 로그는 이벤트 조회에 안 보임)
        String newSessionId = java.util.UUID.randomUUID().toString();
        exchange.getResponse().addCookie(
                org.springframework.http.ResponseCookie.from(SESSION_COOKIE_NAME, newSessionId)
                        .maxAge(java.time.Duration.ofDays(7))
                        .path("/")
                        .build());
        model.addAttribute("title", "로그 업로드 - EVLO");
        model.addAttribute("contentTemplate", "upload");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("showEventsNav", false);
        return "layout/base";
    }

    /**
     * 분석 페이지 → 이벤트 조회 페이지로 리다이렉트 (통합)
     */
    @GetMapping("/analysis")
    public String analysisPage() {
        return "redirect:/events";
    }

    /**
     * 이벤트 리스트 페이지 (검색/필터링 + 분석 통합)
     */
    @GetMapping("/events")
    public String eventsPage(
            ServerWebExchange exchange,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String[] levels,
            @RequestParam(required = false) String[] channels,
            @RequestParam(required = false) Long[] eventIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long logFileId,
            Model model) {

        var sessionCookie = exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME);
        String sessionId = (sessionCookie != null && sessionCookie.getValue() != null) ? sessionCookie.getValue() : null;

        // 검색 요청 객체 생성
        EventSearchRequest searchRequest = EventSearchRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 20)
                .sortBy(sortBy != null ? sortBy : "timeCreated")
                .sortDir(sortDir != null ? sortDir : "DESC")
                .startTime(startTime)
                .endTime(endTime)
                .levels(levels != null ? Arrays.stream(levels)
                        .map(EventLevel::valueOf)
                        .collect(Collectors.toList()) : null)
                .channels(channels != null ? Arrays.stream(channels)
                        .map(LogChannel::valueOf)
                        .collect(Collectors.toList()) : null)
                .eventIds(eventIds != null ? Arrays.asList(eventIds) : null)
                .keyword(keyword)
                .logFileId(logFileId)
                .build();

        // 정렬 설정
        Sort sort = searchRequest.getSortDir().equalsIgnoreCase("ASC")
                ? Sort.by(searchRequest.getSortBy()).ascending()
                : Sort.by(searchRequest.getSortBy()).descending();

        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        // 검색 실행: 비회원은 현재 세션에서 업로드한 로그만 조회 (logFileId 있으면 해당 파일만)
        Page<Event> eventPage;
        if (searchRequest.getLogFileId() != null) {
            eventPage = eventRepository.findByLogFileId(searchRequest.getLogFileId(), pageable);
        } else if (sessionId == null || sessionId.isBlank()) {
            eventPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        } else if (hasFilters(searchRequest)) {
            eventPage = eventRepository.findByFiltersAndSessionId(
                    sessionId,
                    searchRequest.getStartTime(),
                    searchRequest.getEndTime(),
                    searchRequest.getLevels(),
                    searchRequest.getChannels(),
                    searchRequest.getEventIds(),
                    searchRequest.getKeyword(),
                    pageable
            );
        } else {
            eventPage = eventRepository.findByLogFile_SessionId(sessionId, pageable);
        }

        // 캐시 저장 (비동기로 처리, 세션별 키)
        cacheService.cacheSearchResult(searchRequest, pageable, eventPage, sessionId)
                .subscribe();
        
        // 검색 카운트 증가 (통계용)
        cacheService.incrementSearchCount(searchRequest)
                .subscribe();

        // 모델에 데이터 추가
        model.addAttribute("title", "이벤트 조회 - EVLO");
        model.addAttribute("contentTemplate", "events");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("showEventsNav", false);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", eventPage.getNumber());
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("totalElements", eventPage.getTotalElements());
        model.addAttribute("pageSize", searchRequest.getSize());
        model.addAttribute("sortBy", searchRequest.getSortBy());
        model.addAttribute("sortDir", searchRequest.getSortDir());
        
        // 검색 조건
        model.addAttribute("searchRequest", searchRequest);
        model.addAttribute("allLevels", EventLevel.values());
        model.addAttribute("allChannels", LogChannel.values());

        return "layout/base";
    }

    /**
     * 필터가 있는지 확인
     */
    private boolean hasFilters(EventSearchRequest searchRequest) {
        return searchRequest.getStartTime() != null
                || searchRequest.getEndTime() != null
                || (searchRequest.getLevels() != null && !searchRequest.getLevels().isEmpty())
                || (searchRequest.getChannels() != null && !searchRequest.getChannels().isEmpty())
                || (searchRequest.getEventIds() != null && !searchRequest.getEventIds().isEmpty())
                || (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isEmpty());
    }
}
