package com.heartbeat.monitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bpm_records")
@Data
@NoArgsConstructor
public class BpmRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer bpm;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public BpmRecord(Long userId, Integer bpm) {
        this.userId = userId;
        this.bpm = bpm;
        this.timestamp = LocalDateTime.now();
    }
}
