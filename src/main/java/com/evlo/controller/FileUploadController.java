package com.evlo.controller;

import com.evlo.dto.FileUploadResponse;
import com.evlo.entity.LogFile;
import com.evlo.exception.FileValidationException;
import com.evlo.service.FileUploadService;
import com.evlo.support.InMemoryMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * FilePart(application/octet-stream 등)를 MultipartFile로 변환.
     * WebFlux에서 EVTX 등 바이너리 업로드 시 part Content-Type이 octet-stream이라
     * MultipartFile[]로 직접 바인딩되지 않으므로 FilePart로 받아 변환.
     */
    private static Mono<InMemoryMultipartFile> filePartToMultipartFile(FilePart part) {
        return DataBufferUtils.join(part.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .map(bytes -> new InMemoryMultipartFile(
                        bytes,
                        part.name(),
                        part.filename(),
                        part.headers().getContentType() != null ? part.headers().getContentType().toString() : "application/octet-stream"));
    }

    /**
     * 단일 파일 업로드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<FileUploadResponse>> uploadFile(
            @RequestBody reactor.core.publisher.Flux<Part> parts) {

        Mono<ResponseEntity<FileUploadResponse>> mono = parts
                .filter(p -> "file".equals(p.name()) && p instanceof FilePart)
                .cast(FilePart.class)
                .next()
                .flatMap(FileUploadController::filePartToMultipartFile)
                .flatMap(file -> fileUploadService.processFileAsync(file)
                        .map(logFile -> {
                            FileUploadResponse response = FileUploadResponse.builder()
                                    .fileId(logFile.getId())
                                    .filename(logFile.getFilename())
                                    .fileSize(logFile.getFileSize())
                                    .status(logFile.getParsingStatus().name())
                                    .message("File uploaded and parsed successfully")
                                    .build();
                            return ResponseEntity.ok(response);
                        }))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest()
                        .body(FileUploadResponse.builder().status("FAILED").message("No file part").build())));

        mono = mono.onErrorResume(FileValidationException.class, e -> {
            log.error("File validation error: {}", e.getMessage());
            FileUploadResponse response = FileUploadResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
            return Mono.just(ResponseEntity.badRequest().body(response));
        });

        mono = mono.onErrorResume(Exception.class, e -> {
            log.error("Error uploading file: {}", e.getMessage(), e);
            FileUploadResponse response = FileUploadResponse.builder()
                    .status("FAILED")
                    .message("Internal server error: " + e.getMessage())
                    .build();
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        });

        return mono;
    }

    /**
     * 다중 파일 업로드 (FilePart 사용: application/octet-stream 등 EVTX 업로드 지원)
     */
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<List<FileUploadResponse>>> uploadMultipleFiles(
            @RequestBody reactor.core.publisher.Flux<Part> parts) {

        Mono<List<InMemoryMultipartFile>> filesMono = parts
                .filter(p -> "files".equals(p.name()) && p instanceof FilePart)
                .cast(FilePart.class)
                .flatMap(FileUploadController::filePartToMultipartFile)
                .collectList();

        return filesMono
                .flatMap(files -> Mono.fromCallable(() -> {
                    List<FileUploadResponse> responses = new ArrayList<>();
                    for (InMemoryMultipartFile file : files) {
                        try {
                            LogFile logFile = fileUploadService.processFile(file);
                            responses.add(FileUploadResponse.builder()
                                    .fileId(logFile.getId())
                                    .filename(logFile.getFilename())
                                    .fileSize(logFile.getFileSize())
                                    .status(logFile.getParsingStatus().name())
                                    .message("File uploaded and parsed successfully")
                                    .build());
                        } catch (FileValidationException e) {
                            log.error("File validation error for {}: {}", file.getOriginalFilename(), e.getMessage());
                            responses.add(FileUploadResponse.builder()
                                    .filename(file.getOriginalFilename())
                                    .fileSize(file.getSize())
                                    .status("FAILED")
                                    .message(e.getMessage())
                                    .build());
                        } catch (Exception e) {
                            log.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                            responses.add(FileUploadResponse.builder()
                                    .filename(file.getOriginalFilename())
                                    .fileSize(file.getSize())
                                    .status("FAILED")
                                    .message("Internal server error: " + e.getMessage())
                                    .build());
                        }
                    }
                    return ResponseEntity.<List<FileUploadResponse>>ok(responses);
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
