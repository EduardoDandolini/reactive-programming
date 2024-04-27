package com.webflux.reactive.models;

import lombok.Builder;
import lombok.Data;
import lombok.With;

@Builder
@Data
@With
public class Payment {

    private String id;
    private String userId;
    private PayementStatus status;

    public enum PayementStatus {
        PENDING, APPROVED
    }
}
