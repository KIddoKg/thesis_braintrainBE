package com.braintrain.backend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameName {
    // game type: MEMORY
    POSITION(1),
    NEW_PICTURE(3),
    LOST_PICTURE(3),

    // game type: ATTENTION
    DIFFERENCE(1),
    PAIRING(3),
    FISHING(10),

    // game type: LANGUAGE
    STARTING_LETTER(1),
    STARTING_WORD(1),
    NEXT_WORD(1),
    LETTERS_REARRANGE(1),

    // game type: MATH
    SMALLER_EXPRESSION(1),
    SUM(3);

    private final Integer noOfLevels;
}
