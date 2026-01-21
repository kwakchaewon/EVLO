package com.evlo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Long fileId;
    private Double progress;
    private Integer currentCount;
    private Integer totalCount;
    private String status;
}
