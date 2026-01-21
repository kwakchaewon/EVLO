package com.evlo.parser;

import com.evlo.entity.Event;
import com.evlo.entity.LogFile;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.evlo.entity.enums.ParsingStatus;
import com.github.palindromicity.simpleevtx.EvtxParser;
import com.github.palindromicity.simpleevtx.dom.XmlElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EvtxParserService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * EVTX 파일을 파싱하여 Event 엔티티 리스트로 변환
     * 
     * @param evtxFile EVTX 파일
     * @param logFile LogFile 엔티티
     * @return Event 엔티티 리스트
     * @throws IOException 파싱 중 오류 발생 시
     */
    public List<Event> parseEvtxFile(File evtxFile, LogFile logFile) throws IOException {
        List<Event> events = new ArrayList<>();
        
        try (EvtxParser parser = new EvtxParser(evtxFile)) {
            parser.stream().forEach(xmlElement -> {
                try {
                    Event event = convertToEvent(xmlElement, logFile);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse event: {}", e.getMessage());
                }
            });
        }
        
        return events;
    }

    /**
     * XML Element를 Event 엔티티로 변환
     * 
     * @param xmlElement XML Element
     * @param logFile LogFile 엔티티
     * @return Event 엔티티
     */
    private Event convertToEvent(XmlElement xmlElement, LogFile logFile) {
        try {
            Map<String, String> attributes = xmlElement.getAttributes();
            
            // Event ID 추출
            Long eventId = extractLongValue(attributes, "EventID");
            if (eventId == null) {
                return null;
            }

            // Level 추출 및 변환
            EventLevel level = extractEventLevel(attributes.get("Level"));

            // TimeCreated 추출
            LocalDateTime timeCreated = extractTimeCreated(attributes.get("TimeCreated"));

            // Provider 추출
            String provider = attributes.get("Provider");

            // Computer 추출
            String computer = extractComputer(xmlElement);

            // Message 추출
            String message = extractMessage(xmlElement);

            // Channel 추출
            LogChannel channel = extractLogChannel(xmlElement);

            return Event.builder()
                    .eventId(eventId)
                    .level(level != null ? level : EventLevel.INFORMATION)
                    .timeCreated(timeCreated != null ? timeCreated : LocalDateTime.now())
                    .provider(provider)
                    .computer(computer)
                    .message(message)
                    .channel(channel != null ? channel : LogChannel.SYSTEM)
                    .logFile(logFile)
                    .build();

        } catch (Exception e) {
            log.error("Error converting XML element to Event: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Level 문자열을 EventLevel enum으로 변환
     */
    private EventLevel extractEventLevel(String levelStr) {
        if (levelStr == null) {
            return EventLevel.INFORMATION;
        }

        try {
            int levelValue = Integer.parseInt(levelStr.trim());
            // Windows Event Log Level 값:
            // 0 = Information, 1 = Warning, 2 = Error, 3 = Critical
            return switch (levelValue) {
                case 1 -> EventLevel.WARNING;
                case 2 -> EventLevel.ERROR;
                case 3 -> EventLevel.CRITICAL;
                default -> EventLevel.INFORMATION;
            };
        } catch (NumberFormatException e) {
            // 문자열 매핑 시도
            String upper = levelStr.toUpperCase();
            try {
                return EventLevel.valueOf(upper);
            } catch (IllegalArgumentException ex) {
                return EventLevel.INFORMATION;
            }
        }
    }

    /**
     * TimeCreated 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime extractTimeCreated(String timeCreatedStr) {
        if (timeCreatedStr == null || timeCreatedStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // ISO 8601 형식 파싱 시도
            return LocalDateTime.parse(timeCreatedStr.replace(" ", "T"));
        } catch (Exception e) {
            try {
                // 다른 형식 시도
                return LocalDateTime.parse(timeCreatedStr, DATE_TIME_FORMATTER);
            } catch (Exception ex) {
                log.warn("Failed to parse TimeCreated: {}", timeCreatedStr);
                return LocalDateTime.now();
            }
        }
    }

    /**
     * Computer 정보 추출
     */
    private String extractComputer(XmlElement xmlElement) {
        // System 섹션에서 Computer 추출 시도
        XmlElement system = xmlElement.getChild("System");
        if (system != null) {
            XmlElement computerElement = system.getChild("Computer");
            if (computerElement != null) {
                return computerElement.getTextContent();
            }
        }
        return null;
    }

    /**
     * Message 추출
     */
    private String extractMessage(XmlElement xmlElement) {
        // EventData 또는 UserData에서 메시지 추출
        XmlElement eventData = xmlElement.getChild("EventData");
        if (eventData != null) {
            StringBuilder messageBuilder = new StringBuilder();
            for (XmlElement data : eventData.getChildren("Data")) {
                String name = data.getAttribute("Name");
                String value = data.getTextContent();
                if (value != null && !value.isEmpty()) {
                    if (messageBuilder.length() > 0) {
                        messageBuilder.append("; ");
                    }
                    if (name != null) {
                        messageBuilder.append(name).append(": ");
                    }
                    messageBuilder.append(value);
                }
            }
            if (messageBuilder.length() > 0) {
                return messageBuilder.toString();
            }
        }

        // System 섹션에서 메시지 추출 시도
        XmlElement system = xmlElement.getChild("System");
        if (system != null) {
            XmlElement messageElement = system.getChild("Message");
            if (messageElement != null) {
                return messageElement.getTextContent();
            }
        }

        return null;
    }

    /**
     * Log Channel 추출
     */
    private LogChannel extractLogChannel(XmlElement xmlElement) {
        XmlElement system = xmlElement.getChild("System");
        if (system != null) {
            XmlElement channelElement = system.getChild("Channel");
            if (channelElement != null) {
                String channelName = channelElement.getTextContent();
                if (channelName != null) {
                    try {
                        // Channel 이름을 LogChannel enum으로 매핑
                        String normalized = channelName.toUpperCase()
                                .replace("-", "_")
                                .replace(" ", "_");
                        return LogChannel.valueOf(normalized);
                    } catch (IllegalArgumentException e) {
                        // 기본값 반환
                        return LogChannel.SYSTEM;
                    }
                }
            }
        }
        return LogChannel.SYSTEM;
    }

    /**
     * 문자열을 Long으로 변환
     */
    private Long extractLongValue(Map<String, String> attributes, String key) {
        String value = attributes.get(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse {} as Long: {}", key, value);
            return null;
        }
    }
}
