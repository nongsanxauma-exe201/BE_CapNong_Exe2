package com.swd301.foodmarket.exception;

import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.swd301.foodmarket.dto.response.ApiResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";
    private static final String MAX_ATTRIBUTE = "max";

    /**
     * Handle all uncategorized exceptions
     * 🔥 CRITICAL: Log full stack trace to debug
     */
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handleException(Exception ex) {
        // Log full exception with stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        log.error("❌ Uncategorized Exception occurred: ", ex);
        log.error("Full stack trace: {}", sw.toString());

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());

        // 🔥 Return actual exception message for debugging
        // In production, you should return generic message
        apiResponse.setMessage(ex.getMessage() != null ? ex.getMessage() : ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    /**
     * Handle custom AppException
     */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException: {} - {}", errorCode.getCode(), errorCode.getMessage());

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    /**
     * Handle Access Denied (403)
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        log.warn("Access Denied: {}", ex.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
        );
    }

    /**
     * Handle validation errors (400)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        String enumkey = ex.getBindingResult().getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumkey);

            var contrainViolation = ex.getBindingResult()
                    .getAllErrors()
                    .getFirst().unwrap(ConstraintViolation.class);

            attributes = contrainViolation.getConstraintDescriptor().getAttributes();
            log.info("Validation attributes: {}", attributes);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid error code enum: {}", enumkey);
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(Objects.nonNull(attributes) ?
                mapAttribute(errorCode.getMessage(), attributes) :
                errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    /**
     * Map attributes like {min}, {max} into error message
     */
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        String maxValue = String.valueOf(attributes.get(MAX_ATTRIBUTE));

        return message
                .replace("{" + MIN_ATTRIBUTE + "}", minValue)
                .replace("{" + MAX_ATTRIBUTE + "}", maxValue);
    }
}