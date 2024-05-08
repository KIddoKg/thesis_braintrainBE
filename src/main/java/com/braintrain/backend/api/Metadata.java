package com.braintrain.backend.api;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Metadata {
    private boolean success;
    private long totalElements;
    private int pageNumber;
    private int pageSize;
}
