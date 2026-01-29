package com.evlo.parser;

import com.evlo.config.EvtxServiceProperties;
import com.evlo.dto.evtx.EvtxEventDto;
import com.evlo.dto.evtx.EvtxParseResponse;
import com.evlo.entity.Event;
import com.evlo.entity.LogFile;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EvtxParserService {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    private final WebClient webClient;
    private final EvtxServiceProperties props;

    public EvtxParserService(
            @Qualifier("evtxWebClient") WebClient webClient,
            EvtxServiceProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    /**
     * EVTX 파일을 evtx-service에 전달하여 파싱 후 Event 엔티티 리스트로 변환
     */
    public List<Event> parseEvtxFile(File evtxFile, LogFile logFile) throws IOException {
        if (evtxFile == null || !evtxFile.exists()) {
            throw new EvtxParsingException("EVTX file is null or does not exist");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(evtxFile));

        try {
            List<EvtxEventDto> dtos = webClient.post()
                    .uri(ub -> ub.path("/parse").build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(EvtxParseResponse.class)
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .retryWhen(Retry.fixedDelay(
                                    Math.min(3, props.getRetry().getMaxAttempts()),
                                    Duration.ofMillis(props.getRetry().getWaitDuration())
                            )
                            .filter(EvtxParserService::isRetryable)
                            .doBeforeRetry(s -> log.warn("Evtx-service call failed, retrying: {}", s.failure().getMessage()))
                    )
                    .map(EvtxParseResponse::getEvents)
                    .map(list -> list != null ? list : Collections.emptyList())
                    .blockOptional()
                    .orElse(Collections.emptyList());

            return dtos.stream()
                    .map(dto -> convertToEvent(dto, logFile))
                    .collect(Collectors.toList());
        } catch (WebClientResponseException e) {
            log.error("Evtx-service error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EvtxParsingException(
                    "Evtx-service failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            if (e.getCause() instanceof EvtxParsingException) {
                throw (EvtxParsingException) e.getCause();
            }
            log.error("Evtx parsing failed", e);
            throw new EvtxParsingException("Evtx parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * evtx-service가 접근 가능한 파일 경로로 파싱 요청 (공유 볼륨 등)
     */
    public List<Event> parseEvtxFileByPath(String filePath, LogFile logFile, Integer maxEvents, Integer offset) {
        try {
            List<EvtxEventDto> dtos = webClient.post()
                    .uri(ub -> {
                        ub.path("/parse").queryParam("filePath", filePath);
                        if (maxEvents != null) ub.queryParam("maxEvents", maxEvents);
                        if (offset != null) ub.queryParam("offset", offset);
                        return ub.build();
                    })
                    .retrieve()
                    .bodyToMono(EvtxParseResponse.class)
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .map(EvtxParseResponse::getEvents)
                    .map(list -> list != null ? list : Collections.emptyList())
                    .blockOptional()
                    .orElse(Collections.emptyList());

            return dtos.stream()
                    .map(dto -> convertToEvent(dto, logFile))
                    .collect(Collectors.toList());
        } catch (WebClientResponseException e) {
            log.error("Evtx-service error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EvtxParsingException(
                    "Evtx-service failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private static boolean isRetryable(Throwable t) {
        if (t instanceof WebClientResponseException e) {
            int code = e.getStatusCode().value();
            return code >= 500;
        }
        return true;
    }

    private Event convertToEvent(EvtxEventDto dto, LogFile logFile) {
        return Event.builder()
                .eventId(dto.getEventId() != null ? dto.getEventId().longValue() : 0L)
                .level(extractEventLevel(dto.getLevel()))
                .timeCreated(extractTimeCreated(dto.getTimeCreated()))
                .provider(truncate(dto.getProvider(), 500))
                .computer(truncate(dto.getComputer(), 255))
                .channel(extractLogChannel(dto.getChannel()))
                .message(dto.getMessage())
                .logFile(logFile)
                .build();
    }

    private EventLevel extractEventLevel(String levelStr) {
        if (levelStr == null || levelStr.isEmpty()) {
            return EventLevel.INFORMATION;
        }
        String upper = levelStr.trim().toUpperCase();
        if (upper.equals("WARNING")) return EventLevel.WARNING;
        if (upper.equals("ERROR")) return EventLevel.ERROR;
        if (upper.equals("CRITICAL")) return EventLevel.CRITICAL;
        return EventLevel.INFORMATION;
    }

    private LocalDateTime extractTimeCreated(String timeCreatedStr) {
        if (timeCreatedStr == null || timeCreatedStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            String normalized = timeCreatedStr.replace(" ", "T");
            if (normalized.endsWith("Z")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return LocalDateTime.parse(normalized, ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse TimeCreated: {}", timeCreatedStr);
            return LocalDateTime.now();
        }
    }

    private LogChannel extractLogChannel(String channelStr) {
        if (channelStr == null || channelStr.isEmpty()) {
            return LogChannel.SYSTEM;
        }
        String upper = channelStr.trim().toUpperCase().replace("-", "_");
        try {
            return LogChannel.valueOf(upper);
        } catch (IllegalArgumentException e) {
            if (upper.contains("APPLICATION")) return LogChannel.APPLICATION;
            if (upper.contains("SECURITY")) return LogChannel.SECURITY;
            if (upper.contains("SETUP")) return LogChannel.SETUP;
            if (upper.contains("FORWARDED")) return LogChannel.FORWARDED_EVENTS;
            return LogChannel.SYSTEM;
        }
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
