package com.braintrain.backend.api;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Api<T> {
    private Metadata metadata;
    private Error error;
    private T data;
}
