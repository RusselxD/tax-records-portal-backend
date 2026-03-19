package com.taxrecordsportal.tax_records_portal_backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception
    ){
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClientInfoSectionException.class)
    public ResponseEntity<Map<String, Object>> handleClientInfoSectionException(
            ClientInfoSectionException exception
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", exception.getMessage());

        if (exception.getFieldPath() != null) {
            body.put("errors", Map.of(exception.getFieldPath(), exception.getDetail()));
        } else {
            body.put("errors", Map.of("_root", exception.getDetail()));
        }

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        Map<String, String> error = Map.of("message", exception.getReason() != null ? exception.getReason() : "An error occurred");
        return new ResponseEntity<>(error, exception.getStatusCode());
    }

}
