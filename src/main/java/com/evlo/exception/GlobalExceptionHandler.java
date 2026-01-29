package com.evlo.exception;

import com.evlo.dto.ErrorResponse;
import com.evlo.parser.EvtxParsingException;
import com.evlo.exception.EvtxServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

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

    /**
     * 외부 EVTX 파서 서비스(evtx-service) 미동작 시. 사용자에게 관리자 문의 안내.
     */
    @ExceptionHandler(EvtxServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleEvtxServiceUnavailable(EvtxServiceUnavailableException ex, ServerWebExchange exchange) {
        log.warn("Evtx-service unavailable: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "EVTX 파서 서비스를 사용할 수 없습니다. 관리자에게 문의해 주세요.", exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    /**
     * 파일 업로드 등에서 Content-Type이 multipart/form-data가 아닐 때 발생.
     * 지원하지 않는 파일/미디어 타입으로 통일된 메시지 반환.
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        log.debug("Unsupported media type: {} -> {}", exchange.getRequest().getPath(), ex.getReason());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다.", exchange);
    }

    /**
     * 정적 리소스 없음(404). Chrome DevTools의 .well-known 요청은 204로 응답해 로그/500 방지.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (path != null && path.contains(".well-known")) {
            log.debug("Well-known path (e.g. Chrome DevTools): {} -> 204", path);
            return ResponseEntity.noContent().build();
        }
        log.debug("Resource not found: {}", path);
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", exchange);
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
