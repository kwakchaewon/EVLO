package com.evlo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryResponse {
    private String summary;
    private String summaryType;
    private String errorMessage;
}
