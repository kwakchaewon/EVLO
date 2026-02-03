package com.evlo.entity;

import com.evlo.entity.enums.ParsingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String filename;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParsingStatus parsingStatus = ParsingStatus.IN_PROGRESS;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 비회원 세션 구분 (쿠키 EVLO_SESSION). 추후 회원이면 user_id 사용 예정 */
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @OneToMany(mappedBy = "logFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
