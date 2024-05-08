package com.braintrain.backend.dto;

import com.braintrain.backend.entity.ObjectiveType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObjectiveDto {
    private UUID id;
    private ObjectiveType objectiveType;
    private int level;
    private double quantityRequired;
    private String title;
    private String description;
    private boolean isAchieved;
    private Long achievedDate;
}
