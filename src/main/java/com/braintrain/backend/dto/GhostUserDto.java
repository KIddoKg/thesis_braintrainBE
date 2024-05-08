package com.braintrain.backend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GhostUserDto {
    private UUID id;
    private String fullName;
    private String phone;
    private long latestPlayDate;
    private int noPlayDays;
}
