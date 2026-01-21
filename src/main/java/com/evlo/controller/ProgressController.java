package com.evlo.controller;

import com.evlo.dto.ProgressResponse;
import com.evlo.entity.LogFile;
import com.evlo.entity.enums.ParsingStatus;
import com.evlo.repository.LogFileRepository;
import com.evlo.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressTrackingService progressTrackingService;
    private final LogFileRepository logFileRepository;

    /**
     * 파일 업로드/파싱 진행률 조회
     */
    @GetMapping("/{fileId}")
    public Mono<ResponseEntity<ProgressResponse>> getProgress(@PathVariable Long fileId) {
        return progressTrackingService.getProgress(fileId)
                .flatMap(progress -> {
                    // LogFile 상태 확인
                    return Mono.fromCallable(() -> {
                        LogFile logFile = logFileRepository.findById(fileId)
                                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
                        
                        ParsingStatus status = logFile.getParsingStatus();
                        String statusStr = status.name();
                        
                        // 완료 시 진행률 100%로 설정
                        double finalProgress = (status == ParsingStatus.COMPLETED) ? 100.0 : progress;
                        
                        ProgressResponse response = ProgressResponse.builder()
                                .fileId(fileId)
                                .progress(finalProgress)
                                .status(statusStr)
                                .build();
                        
                        return ResponseEntity.ok(response);
                    })
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
