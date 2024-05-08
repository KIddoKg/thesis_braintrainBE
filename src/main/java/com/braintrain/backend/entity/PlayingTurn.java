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
public class PlayingTurn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    private GameName gameName;

    private Integer noOfLevels;

    private Integer score;

    private Integer playTime;

    private Integer level;

    private Integer newPicOneResult;

    private Integer newPicTwoResult;

    private Integer noOfFishCaught;

    private Boolean boatStatus;

    private String wordList;

    private LocalDateTime createdDate;

    @ManyToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;
}
