package com.braintrain.backend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRanking {
    private UUID userId;
    private String userName;
    private int score;
}
