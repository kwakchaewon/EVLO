package com.evlo.service;

import com.evlo.dto.AiSummaryRequest;
import com.evlo.dto.AiSummaryResponse;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final ChatClient chatClient;
    private final EventRepository eventRepository;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * 전체 로그 요약
     */
    public AiSummaryResponse summarizeEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return AiSummaryResponse.builder()
                    .summary("분석할 이벤트가 없습니다.")
                    .summaryType("full")
                    .build();
        }

        try {
            String eventSummary = buildEventSummary(events);
            
            PromptTemplate promptTemplate = new PromptTemplate("""
                다음은 Windows 이벤트 로그 데이터입니다. 
                이벤트들을 분석하여 간결하고 명확한 요약을 제공해주세요.
                
                요약 시 다음 사항을 포함해주세요:
                1. 전체 이벤트 개수 및 주요 이벤트 유형
                2. Error 또는 Critical 레벨의 이벤트가 있다면 강조
                3. 시간대별 발생 패턴 (있는 경우)
                4. 주의가 필요한 사항
                
                이벤트 데이터:
                {events}
                
                한국어로 요약해주세요.
                """);

            Prompt prompt = promptTemplate.create(Map.of("events", eventSummary));
            String response = chatClient.call(prompt).getResult().getOutput().getContent();

            return AiSummaryResponse.builder()
                    .summary(response)
                    .summaryType("full")
                    .build();

        } catch (Exception e) {
            log.error("Error generating AI summary: {}", e.getMessage(), e);
            return AiSummaryResponse.builder()
                    .summary("AI 요약 생성 중 오류가 발생했습니다.")
                    .summaryType("full")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Error/Critical 이벤트 중심 요약
     */
    public AiSummaryResponse summarizeErrors(List<Event> errorEvents) {
        if (errorEvents == null || errorEvents.isEmpty()) {
            return AiSummaryResponse.builder()
                    .summary("Error 또는 Critical 레벨의 이벤트가 없습니다.")
                    .summaryType("error")
                    .build();
        }

        try {
            String eventSummary = buildEventSummary(errorEvents);
            
            PromptTemplate promptTemplate = new PromptTemplate("""
                다음은 Windows 이벤트 로그에서 발생한 Error 및 Critical 레벨의 이벤트들입니다.
                이 이벤트들을 분석하여 장애 원인을 추정하고 해결 방안을 제시해주세요.
                
                분석 시 다음 사항을 포함해주세요:
                1. 주요 에러 유형 및 발생 빈도
                2. 가능한 장애 원인 추정
                3. 권장 해결 방안
                4. 우선순위 (긴급도 기준)
                
                이벤트 데이터:
                {events}
                
                한국어로 분석해주세요.
                """);

            Prompt prompt = promptTemplate.create(Map.of("events", eventSummary));
            String response = chatClient.call(prompt).getResult().getOutput().getContent();

            return AiSummaryResponse.builder()
                    .summary(response)
                    .summaryType("error")
                    .build();

        } catch (Exception e) {
            log.error("Error generating error summary: {}", e.getMessage(), e);
            return AiSummaryResponse.builder()
                    .summary("Error 요약 생성 중 오류가 발생했습니다.")
                    .summaryType("error")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 특정 Event ID 설명 생성 (초보자용)
     */
    public AiSummaryResponse explainEventId(Long eventId) {
        try {
            List<Event> events = eventRepository.findByEventId(eventId, 
                    org.springframework.data.domain.PageRequest.of(0, 5)).getContent();

            if (events.isEmpty()) {
                return AiSummaryResponse.builder()
                        .summary("Event ID " + eventId + "에 대한 이벤트를 찾을 수 없습니다.")
                        .summaryType("explain")
                        .build();
            }

            Event sampleEvent = events.get(0);
            String eventDescription = String.format(
                    "Event ID: %d\nLevel: %s\nProvider: %s\nMessage: %s\nComputer: %s",
                    sampleEvent.getEventId(),
                    sampleEvent.getLevel(),
                    sampleEvent.getProvider() != null ? sampleEvent.getProvider() : "N/A",
                    sampleEvent.getMessage() != null ? sampleEvent.getMessage() : "N/A",
                    sampleEvent.getComputer() != null ? sampleEvent.getComputer() : "N/A"
            );

            PromptTemplate promptTemplate = new PromptTemplate("""
                다음은 Windows 이벤트 로그의 Event ID {eventId}에 대한 정보입니다.
                비전문가도 이해할 수 있도록 이 이벤트를 간단하고 명확하게 설명해주세요.
                
                설명 시 다음 사항을 포함해주세요:
                1. 이 이벤트가 무엇인지
                2. 어떤 상황에서 발생하는지
                3. 주의가 필요한지 (중요도)
                4. 일반적인 원인 (있는 경우)
                
                이벤트 정보:
                {eventInfo}
                
                한국어로 쉽게 설명해주세요.
                """);

            Prompt prompt = promptTemplate.create(Map.of(
                    "eventId", eventId.toString(),
                    "eventInfo", eventDescription
            ));
            
            String response = chatClient.call(prompt).getResult().getOutput().getContent();

            return AiSummaryResponse.builder()
                    .summary(response)
                    .summaryType("explain")
                    .build();

        } catch (Exception e) {
            log.error("Error generating event explanation: {}", e.getMessage(), e);
            return AiSummaryResponse.builder()
                    .summary("Event ID 설명 생성 중 오류가 발생했습니다.")
                    .summaryType("explain")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 이벤트 리스트를 요약 가능한 텍스트로 변환
     */
    private String buildEventSummary(List<Event> events) {
        if (events.isEmpty()) {
            return "이벤트 없음";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("총 이벤트 개수: %d\n\n", events.size()));

        // Event ID별 그룹화
        Map<Long, Long> eventIdCounts = events.stream()
                .collect(Collectors.groupingBy(Event::getEventId, Collectors.counting()));

        summary.append("Event ID별 발생 빈도:\n");
        eventIdCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> 
                    summary.append(String.format("  Event ID %d: %d회\n", entry.getKey(), entry.getValue()))
                );

        // Level별 통계
        Map<EventLevel, Long> levelCounts = events.stream()
                .collect(Collectors.groupingBy(Event::getLevel, Collectors.counting()));

        summary.append("\nLevel별 통계:\n");
        levelCounts.forEach((level, count) -> 
            summary.append(String.format("  %s: %d개\n", level, count))
        );

        // 샘플 이벤트 (최대 5개)
        summary.append("\n샘플 이벤트:\n");
        events.stream().limit(5).forEach(event -> {
            summary.append(String.format(
                    "  Event ID %d [%s]: %s\n",
                    event.getEventId(),
                    event.getLevel(),
                    event.getMessage() != null && event.getMessage().length() > 100
                            ? event.getMessage().substring(0, 100) + "..."
                            : event.getMessage() != null ? event.getMessage() : "N/A"
            ));
        });

        return summary.toString();
    }
}
