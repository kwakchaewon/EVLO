package com.evlo.repository;

import com.evlo.entity.LogFile;
import com.evlo.entity.enums.ParsingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogFileRepository extends JpaRepository<LogFile, Long> {

    // 파싱 상태별 조회
    Page<LogFile> findByParsingStatus(ParsingStatus status, Pageable pageable);
    List<LogFile> findByParsingStatus(ParsingStatus status);

    // 파일명으로 조회
    Optional<LogFile> findByFilename(String filename);

    // 업로드 시간 범위로 조회
    Page<LogFile> findByUploadedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 특정 상태의 파일 개수
    long countByParsingStatus(ParsingStatus status);

    // 파싱 상태 업데이트를 위한 메서드 (커스텀)
    @Query("UPDATE LogFile lf SET lf.parsingStatus = :status WHERE lf.id = :id")
    void updateParsingStatus(@Param("id") Long id, @Param("status") ParsingStatus status);

    // 최근 업로드된 파일 조회
    Page<LogFile> findByOrderByUploadedAtDesc(Pageable pageable);

    // 파일명으로 검색
    @Query("SELECT lf FROM LogFile lf WHERE lf.filename LIKE %:keyword%")
    Page<LogFile> findByFilenameContaining(@Param("keyword") String keyword, Pageable pageable);

    // 특정 기간에 업로드된 파일 목록
    @Query("SELECT lf FROM LogFile lf WHERE lf.uploadedAt BETWEEN :startTime AND :endTime ORDER BY lf.uploadedAt DESC")
    List<LogFile> findFilesUploadedBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 파싱 완료된 파일만 조회
    List<LogFile> findByParsingStatusOrderByUploadedAtDesc(ParsingStatus status);
}
