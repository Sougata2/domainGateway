package com.domain.gateway.exception.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class ErrorDto {
    private String message;
    private HttpStatus status;
    private String path;
    private String timestamp;

}