package com.braintrain.backend.api;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Error {
    private String code;
    private String message;
}
