package com.evlo.dto.evtx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvtxParseResponse {

    private List<EvtxEventDto> events;
    private Integer count;

    @JsonProperty("totalCount")
    private Integer totalCount;
}
