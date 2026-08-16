package com.orderflow.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Instant timestamp;
    private String service;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String method;
    private String traceId;
    private String requestId;
    private List<ValidationError> errors;
}
