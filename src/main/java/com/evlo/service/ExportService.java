package com.evlo.service;

import com.evlo.dto.EventSearchRequest;
import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.evlo.repository.EventRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final EventRepository eventRepository;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 필터 조건 생성
     */
    public EventSearchRequest buildSearchRequest(Integer size,
                                                 String sortBy,
                                                 String sortDir,
                                                 java.time.LocalDateTime startTime,
                                                 java.time.LocalDateTime endTime,
                                                 String[] levels,
                                                 String[] channels,
                                                 Long[] eventIds,
                                                 String keyword,
                                                 Long logFileId) {
        return EventSearchRequest.builder()
                .page(0)
                .size(size != null ? size : 500)
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
    }

    /**
     * 필터 여부 확인
     */
    private boolean hasFilters(EventSearchRequest searchRequest) {
        return searchRequest.getStartTime() != null
                || searchRequest.getEndTime() != null
                || (searchRequest.getLevels() != null && !searchRequest.getLevels().isEmpty())
                || (searchRequest.getChannels() != null && !searchRequest.getChannels().isEmpty())
                || (searchRequest.getEventIds() != null && !searchRequest.getEventIds().isEmpty())
                || (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isEmpty());
    }

    /**
     * 필터 조건에 맞는 이벤트 조회 (sessionId 있으면 비회원 세션 범위로만 조회)
     */
    public List<Event> fetchEvents(EventSearchRequest searchRequest, String sessionId) {
        Sort sort = searchRequest.getSortDir().equalsIgnoreCase("ASC")
                ? Sort.by(searchRequest.getSortBy()).ascending()
                : Sort.by(searchRequest.getSortBy()).descending();

        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

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

        return eventPage.getContent();
    }

    /**
     * CSV 생성
     */
    public byte[] exportCsv(List<Event> events) {
        String header = "EventID,Level,TimeCreated,Provider,Computer,Message\n";
        StringBuilder sb = new StringBuilder(header);

        events.forEach(event -> {
            sb.append(escapeCsv(String.valueOf(event.getEventId()))).append(",");
            sb.append(escapeCsv(event.getLevel().name())).append(",");
            sb.append(escapeCsv(event.getTimeCreated() != null ? event.getTimeCreated().format(TIME_FORMAT) : "")).append(",");
            sb.append(escapeCsv(event.getProvider())).append(",");
            sb.append(escapeCsv(event.getComputer())).append(",");
            sb.append(escapeCsv(event.getMessage())).append("\n");
        });

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * JSON 변환은 컨트롤러에서 직접 반환 (Jackson)
     */

    /**
     * PDF 생성
     */
    public byte[] exportPdf(List<Event> events) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font textFont = new Font(Font.HELVETICA, 10);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            document.add(new Paragraph("EVLO 분석 보고서", titleFont));
            document.add(new Paragraph("총 이벤트 개수: " + events.size(), textFont));
            document.add(new Paragraph(" ", textFont));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 2f, 3f, 3f, 3f, 6f});

            addHeaderCell(table, "Event ID", headerFont);
            addHeaderCell(table, "Level", headerFont);
            addHeaderCell(table, "Time", headerFont);
            addHeaderCell(table, "Provider", headerFont);
            addHeaderCell(table, "Computer", headerFont);
            addHeaderCell(table, "Message", headerFont);

            for (Event event : events) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(event.getEventId()), textFont)));
                table.addCell(new PdfPCell(new Phrase(event.getLevel().name(), textFont)));
                table.addCell(new PdfPCell(new Phrase(
                        event.getTimeCreated() != null ? event.getTimeCreated().format(TIME_FORMAT) : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(
                        event.getProvider() != null ? event.getProvider() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(
                        event.getComputer() != null ? event.getComputer() : "", textFont)));
                table.addCell(new PdfPCell(new Phrase(
                        event.getMessage() != null ? truncate(event.getMessage(), 500) : "", textFont)));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 236, 240));
        table.addCell(cell);
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }
}
