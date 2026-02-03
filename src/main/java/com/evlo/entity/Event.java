package com.evlo.entity;

import com.evlo.entity.enums.EventLevel;
import com.evlo.entity.enums.LogChannel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_time_created", columnList = "timeCreated"),
    @Index(name = "idx_event_id", columnList = "eventId"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_channel", columnList = "channel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventLevel level;

    @Column(nullable = false)
    private LocalDateTime timeCreated;

    @Column(length = 500)
    private String provider;

    @Column(length = 255)
    private String computer;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_file_id", nullable = false)
    @JsonIgnore
    private LogFile logFile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
