package com.braintrain.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class DailyWorkout {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double sleepHrs;

    private Integer mood;

    private boolean isMemoryPlayed;

    private boolean isAttentionPlayed;

    private boolean isLanguagePlayed;

    private boolean isMathPlayed;

    private LocalDate createdDate;

    @ManyToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
}
