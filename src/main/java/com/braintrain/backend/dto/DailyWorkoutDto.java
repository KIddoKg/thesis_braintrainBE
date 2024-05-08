package com.braintrain.backend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyWorkoutDto {
    private UUID id;
    private Double sleepHrs;
    private Integer mood;
    private boolean isMemoryPlayed;
    private boolean isAttentionPlayed;
    private boolean isLanguagePlayed;
    private boolean isMathPlayed;
}
