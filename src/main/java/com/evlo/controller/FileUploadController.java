package com.evlo.controller;

import com.evlo.dto.FileUploadResponse;
import com.evlo.entity.LogFile;
import com.evlo.exception.FileValidationException;
import com.evlo.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 단일 파일 업로드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<FileUploadResponse>> uploadFile(
            @RequestPart("file") MultipartFile file) {

        return Mono.fromCallable(() -> {
            try {
                LogFile logFile = fileUploadService.processFile(file);
                FileUploadResponse response = FileUploadResponse.builder()
                        .fileId(logFile.getId())
                        .filename(logFile.getFilename())
                        .fileSize(logFile.getFileSize())
                        .status(logFile.getParsingStatus().name())
                        .message("File uploaded and parsed successfully")
                        .build();

                return ResponseEntity.ok(response);
            } catch (FileValidationException e) {
                log.error("File validation error: {}", e.getMessage());
                FileUploadResponse response = FileUploadResponse.builder()
                        .status("FAILED")
                        .message(e.getMessage())
                        .build();
                return ResponseEntity.badRequest().body(response);
            } catch (Exception e) {
                log.error("Error uploading file: {}", e.getMessage(), e);
                FileUploadResponse response = FileUploadResponse.builder()
                        .status("FAILED")
                        .message("Internal server error: " + e.getMessage())
                        .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * 다중 파일 업로드
     */
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<List<FileUploadResponse>>> uploadMultipleFiles(
            @RequestPart("files") MultipartFile[] files) {

        return Mono.fromCallable(() -> {
            List<FileUploadResponse> responses = List.of(files).stream()
                    .map(file -> {
                        try {
                            LogFile logFile = fileUploadService.processFile(file);
                            return FileUploadResponse.builder()
                                    .fileId(logFile.getId())
                                    .filename(logFile.getFilename())
                                    .fileSize(logFile.getFileSize())
                                    .status(logFile.getParsingStatus().name())
                                    .message("File uploaded and parsed successfully")
                                    .build();
                        } catch (FileValidationException e) {
                            log.error("File validation error for {}: {}", file.getOriginalFilename(), e.getMessage());
                            return FileUploadResponse.builder()
                                    .filename(file.getOriginalFilename())
                                    .fileSize(file.getSize())
                                    .status("FAILED")
                                    .message(e.getMessage())
                                    .build();
                        } catch (Exception e) {
                            log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                            return FileUploadResponse.builder()
                                    .filename(file.getOriginalFilename())
                                    .fileSize(file.getSize())
                                    .status("FAILED")
                                    .message("Internal server error: " + e.getMessage())
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}
