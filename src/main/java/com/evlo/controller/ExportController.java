package com.evlo.controller;

import com.evlo.dto.EventSearchRequest;
import com.evlo.entity.Event;
import com.evlo.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private static final String SESSION_COOKIE_NAME = "EVLO_SESSION";
    private final ExportService exportService;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static String getSessionId(ServerWebExchange exchange) {
        var cookie = exchange.getRequest().getCookies().getFirst(SESSION_COOKIE_NAME);
        return cookie != null && cookie.getValue() != null ? cookie.getValue() : null;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            ServerWebExchange exchange,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String[] levels,
            @RequestParam(required = false) String[] channels,
            @RequestParam(required = false) Long[] eventIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long logFileId
    ) {
        EventSearchRequest searchRequest = exportService.buildSearchRequest(size, sortBy, sortDir, startTime, endTime, levels, channels, eventIds, keyword, logFileId);
        List<Event> events = exportService.fetchEvents(searchRequest, getSessionId(exchange));
        byte[] csvBytes = exportService.exportCsv(events);

        String filename = "evlo_events_" + LocalDateTime.now().format(FILE_TIME) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }

    @GetMapping("/json")
    public ResponseEntity<List<Event>> exportJson(
            ServerWebExchange exchange,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String[] levels,
            @RequestParam(required = false) String[] channels,
            @RequestParam(required = false) Long[] eventIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long logFileId
    ) {
        EventSearchRequest searchRequest = exportService.buildSearchRequest(size, sortBy, sortDir, startTime, endTime, levels, channels, eventIds, keyword, logFileId);
        List<Event> events = exportService.fetchEvents(searchRequest, getSessionId(exchange));
        return ResponseEntity.ok(events);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            ServerWebExchange exchange,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String[] levels,
            @RequestParam(required = false) String[] channels,
            @RequestParam(required = false) Long[] eventIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long logFileId
    ) {
        EventSearchRequest searchRequest = exportService.buildSearchRequest(size, sortBy, sortDir, startTime, endTime, levels, channels, eventIds, keyword, logFileId);
        List<Event> events = exportService.fetchEvents(searchRequest, getSessionId(exchange));
        byte[] pdfBytes = exportService.exportPdf(events);

        String filename = "evlo_report_" + LocalDateTime.now().format(FILE_TIME) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private String contentDisposition(String filename) {
        return "attachment; filename=\"" + filename + "\"";
    }
}
