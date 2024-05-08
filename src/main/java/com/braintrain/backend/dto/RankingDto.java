package com.braintrain.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RankingDto {
    private int myRank;
    private List<UserRanking> userRankings;
}
