package com.evlo.service;

import com.evlo.entity.LogFile;
import com.evlo.entity.enums.ParsingStatus;
import com.evlo.exception.FileValidationException;
import com.evlo.parser.EvtxParserService;
import com.evlo.parser.EvtxParsingException;
import com.evlo.repository.EventRepository;
import com.evlo.repository.LogFileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final String EVTX_EXTENSION = ".evtx";
    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024; // 200MB

    private final LogFileRepository logFileRepository;
    private final EventRepository eventRepository;
    private final EvtxParserService evtxParserService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.upload.temp-dir:./temp/uploads}")
    private String tempUploadDir;

    /**
     * 파일 검증 (확장자, 크기)
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty or null");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(EVTX_EXTENSION)) {
            throw new FileValidationException("Invalid file extension. Only .evtx files are allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException(
                    String.format("File size exceeds maximum limit. Max: %d MB, Actual: %.2f MB",
                            MAX_FILE_SIZE / (1024 * 1024),
                            file.getSize() / (1024.0 * 1024.0))
            );
        }
    }

    /**
     * 파일 업로드 및 파싱 처리
     */
    @Transactional
    public LogFile processFile(MultipartFile multipartFile) {
        // 파일 검증
        validateFile(multipartFile);

        String filename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();

        // LogFile 엔티티 생성
        LogFile logFile = LogFile.builder()
                .filename(filename)
                .fileSize(fileSize)
                .parsingStatus(ParsingStatus.IN_PROGRESS)
                .uploadedAt(LocalDateTime.now())
                .build();

        logFile = logFileRepository.save(logFile);

        File tempFile = null;
        try {
            // 임시 디렉토리 생성
            Path tempDir = Paths.get(tempUploadDir);
            Files.createDirectories(tempDir);

            // 임시 파일 저장
            tempFile = new File(tempDir.toFile(), logFile.getId() + "_" + filename);
            multipartFile.transferTo(tempFile);

            // EVTX 파일 파싱
            List<com.evlo.entity.Event> events = evtxParserService.parseEvtxFile(tempFile, logFile);

            // Batch Insert (500-1000건마다 flush/clear)
            int batchSize = 1000;
            for (int i = 0; i < events.size(); i++) {
                entityManager.persist(events.get(i));

                if ((i + 1) % batchSize == 0) {
                    entityManager.flush();
                    entityManager.clear();
                    log.debug("Flushed {} events", i + 1);
                }
            }

            // 남은 이벤트 처리
            if (events.size() % batchSize != 0) {
                entityManager.flush();
                entityManager.clear();
            }

            // 파싱 상태 업데이트
            logFile.setParsingStatus(ParsingStatus.COMPLETED);
            logFileRepository.save(logFile);

            log.info("File processed successfully: {} ({} events)", filename, events.size());
            return logFile;

        } catch (EvtxParsingException e) {
            log.error("Error parsing EVTX file: {}", filename, e);
            logFile.setParsingStatus(ParsingStatus.FAILED);
            logFileRepository.save(logFile);
            throw new FileValidationException("Failed to parse EVTX file: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error processing file: {}", filename, e);
            logFile.setParsingStatus(ParsingStatus.FAILED);
            logFileRepository.save(logFile);
            throw new FileValidationException("Error processing file: " + e.getMessage(), e);
        } finally {
            // 임시 파일 삭제
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }
}
