package com.braintrain.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Objective {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ObjectiveType objectiveType;

    private int level;

    private double quantityRequired;

    private String title;

    private String description;

    private boolean isAchieved;

    private LocalDateTime achievedDate;

    private LocalDateTime createdDate;

    @ManyToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
}
