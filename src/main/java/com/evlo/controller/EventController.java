package com.evlo.controller;

import com.evlo.dto.EventSearchRequest;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.evlo.repository.EventRepository;
import com.evlo.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    public String index() {
        return "index";
    }

    /**
     * 로그 업로드 페이지
     */
    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    /**
     * 분석 페이지
     */
    @GetMapping("/analysis")
    public String analysisPage() {
        return "analysis";
    }

    /**
     * 이벤트 리스트 페이지 (검색/필터링 지원)
     */
    @GetMapping("/events")
    public String eventsPage(
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

        // 검색 실행 (캐시 조회는 우선 DB 조회 후 캐시 저장 방식)
        Page<Event> eventPage;
        if (hasFilters(searchRequest)) {
            // 필터가 있는 경우 복합 검색
            eventPage = eventRepository.findByFilters(
                    searchRequest.getStartTime(),
                    searchRequest.getEndTime(),
                    searchRequest.getLevels(),
                    searchRequest.getChannels(),
                    searchRequest.getEventIds(),
                    searchRequest.getKeyword(),
                    pageable
            );
        } else if (searchRequest.getLogFileId() != null) {
            // LogFile ID만 있는 경우
            eventPage = eventRepository.findByLogFileId(searchRequest.getLogFileId(), pageable);
        } else {
            // 필터 없이 전체 조회
            eventPage = eventRepository.findAll(pageable);
        }

        // 캐시 저장 (비동기로 처리)
        cacheService.cacheSearchResult(searchRequest, pageable, eventPage)
                .subscribe();
        
        // 검색 카운트 증가 (통계용)
        cacheService.incrementSearchCount(searchRequest)
                .subscribe();

        // 모델에 데이터 추가
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

        return "events";
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
