package com.evlo.dto.evtx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvtxEventDto {

    private Integer eventId;
    private String level;
    private String timeCreated;
    private String provider;
    private String computer;
    private String channel;
    private String message;

    @JsonProperty("rawXml")
    private String rawXml;
}
