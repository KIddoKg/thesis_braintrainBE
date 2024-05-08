package com.braintrain.backend.dto;

import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.entity.GameType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayingTurnDto {
    private UUID id;
    private GameType gameType;
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
    private long createdDate;
}
