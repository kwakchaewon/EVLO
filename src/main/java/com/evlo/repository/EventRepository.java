package com.evlo.repository;

import com.evlo.entity.Event;
import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // 기본 조회
    Page<Event> findByLogFileId(Long logFileId, Pageable pageable);

    /** 비회원 세션: 해당 세션에서 업로드한 로그의 이벤트만 조회 */
    Page<Event> findByLogFile_SessionId(String sessionId, Pageable pageable);

    // 기간 필터
    Page<Event> findByTimeCreatedBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Event Level 필터
    Page<Event> findByLevel(EventLevel level, Pageable pageable);
    Page<Event> findByLevelIn(List<EventLevel> levels, Pageable pageable);

    // Log Channel 필터
    Page<Event> findByChannel(LogChannel channel, Pageable pageable);
    Page<Event> findByChannelIn(List<LogChannel> channels, Pageable pageable);

    // Event ID 필터
    Page<Event> findByEventId(Long eventId, Pageable pageable);
    Page<Event> findByEventIdIn(List<Long> eventIds, Pageable pageable);

    // Message 키워드 검색
    @Query("SELECT e FROM Event e WHERE e.message LIKE %:keyword%")
    Page<Event> findByMessageContaining(@Param("keyword") String keyword, Pageable pageable);

    // Message 키워드 검색 (대소문자 구분 안 함)
    @Query("SELECT e FROM Event e WHERE LOWER(e.message) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Event> findByMessageContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    // 복합 검색 (기간 + Level + Channel)
    @Query("SELECT e FROM Event e WHERE " +
           "(:startTime IS NULL OR e.timeCreated >= :startTime) AND " +
           "(:endTime IS NULL OR e.timeCreated <= :endTime) AND " +
           "(:levels IS NULL OR e.level IN :levels) AND " +
           "(:channels IS NULL OR e.channel IN :channels) AND " +
           "(:eventIds IS NULL OR e.eventId IN :eventIds) AND " +
           "(:keyword IS NULL OR LOWER(e.message) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> findByFilters(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("levels") List<EventLevel> levels,
            @Param("channels") List<LogChannel> channels,
            @Param("eventIds") List<Long> eventIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /** 복합 검색 + 세션 제한 (비회원: 이번 세션 로그만) */
    @Query("SELECT e FROM Event e JOIN e.logFile lf WHERE lf.sessionId = :sessionId AND " +
           "(:startTime IS NULL OR e.timeCreated >= :startTime) AND " +
           "(:endTime IS NULL OR e.timeCreated <= :endTime) AND " +
           "(:levels IS NULL OR e.level IN :levels) AND " +
           "(:channels IS NULL OR e.channel IN :channels) AND " +
           "(:eventIds IS NULL OR e.eventId IN :eventIds) AND " +
           "(:keyword IS NULL OR LOWER(e.message) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> findByFiltersAndSessionId(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("levels") List<EventLevel> levels,
            @Param("channels") List<LogChannel> channels,
            @Param("eventIds") List<Long> eventIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Error/Critical Top N 조회
    @Query("SELECT e FROM Event e WHERE e.level IN ('ERROR', 'CRITICAL') ORDER BY e.timeCreated DESC")
    Page<Event> findTopErrorsAndCritical(Pageable pageable);

    @Query("SELECT e FROM Event e JOIN e.logFile lf WHERE lf.sessionId = :sessionId AND e.level IN ('ERROR', 'CRITICAL') ORDER BY e.timeCreated DESC")
    Page<Event> findTopErrorsAndCriticalBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    // Event ID별 발생 빈도
    @Query("SELECT e.eventId, COUNT(e) as count FROM Event e GROUP BY e.eventId ORDER BY count DESC")
    List<Object[]> findEventIdFrequency();

    @Query("SELECT e.eventId, COUNT(e) as count FROM Event e JOIN e.logFile lf WHERE lf.sessionId = :sessionId GROUP BY e.eventId ORDER BY count DESC")
    List<Object[]> findEventIdFrequencyBySessionId(@Param("sessionId") String sessionId);

    // 시간대별 집중 발생 이벤트 (시간 단위)
    @Query("SELECT FUNCTION('DATE_FORMAT', e.timeCreated, '%Y-%m-%d %H:00:00') as timeSlot, " +
           "COUNT(e) as count FROM Event e " +
           "WHERE e.timeCreated BETWEEN :startTime AND :endTime " +
           "GROUP BY timeSlot ORDER BY timeSlot")
    List<Object[]> findEventCountByHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT FUNCTION('DATE_FORMAT', e.timeCreated, '%Y-%m-%d %H:00:00') as timeSlot, " +
           "COUNT(e) as count FROM Event e JOIN e.logFile lf WHERE lf.sessionId = :sessionId " +
           "AND e.timeCreated BETWEEN :startTime AND :endTime GROUP BY timeSlot ORDER BY timeSlot")
    List<Object[]> findEventCountByHourBySessionId(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 특정 Event ID의 시간대별 발생 빈도
    @Query("SELECT FUNCTION('DATE_FORMAT', e.timeCreated, '%Y-%m-%d %H:00:00') as timeSlot, " +
           "COUNT(e) as count FROM Event e " +
           "WHERE e.eventId = :eventId AND e.timeCreated BETWEEN :startTime AND :endTime " +
           "GROUP BY timeSlot ORDER BY timeSlot")
    List<Object[]> findEventCountByHourForEventId(
            @Param("eventId") Long eventId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT FUNCTION('DATE_FORMAT', e.timeCreated, '%Y-%m-%d %H:00:00') as timeSlot, " +
           "COUNT(e) as count FROM Event e JOIN e.logFile lf WHERE lf.sessionId = :sessionId " +
           "AND e.eventId = :eventId AND e.timeCreated BETWEEN :startTime AND :endTime GROUP BY timeSlot ORDER BY timeSlot")
    List<Object[]> findEventCountByHourForEventIdBySessionId(
            @Param("sessionId") String sessionId,
            @Param("eventId") Long eventId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // LogFile ID로 삭제
    void deleteByLogFileId(Long logFileId);
}
