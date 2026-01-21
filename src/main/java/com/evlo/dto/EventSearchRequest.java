package com.evlo.dto;

import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchRequest {

    // 페이징
    @Min(0)
    @Builder.Default
    private int page = 0;

    @Min(1)
    @Builder.Default
    private int size = 20;

    // 정렬
    @Builder.Default
    private String sortBy = "timeCreated";

    @Builder.Default
    private String sortDir = "DESC";

    // 필터 조건
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private List<EventLevel> levels;
    private List<LogChannel> channels;
    
    private List<Long> eventIds;
    
    private String keyword;
    
    private Long logFileId;
}
