package com.ticketing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ErrorResponse {
    private String code;
    private String message;
}
