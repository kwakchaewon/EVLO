package com.evlo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryRequest {
    private List<Long> eventIds;
    private String summaryType; // "full", "error", "explain"
    private Long eventIdForExplanation;
}
