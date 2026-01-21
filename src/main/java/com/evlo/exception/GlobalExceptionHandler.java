package com.evlo.exception;

import com.evlo.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidation(FileValidationException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(EvtxParsingException.class)
    public ResponseEntity<ErrorResponse> handleEvtxParsing(EvtxParsingException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", exchange);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange != null && exchange.getRequest() != null ? exchange.getRequest().getPath().value() : "")
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}
