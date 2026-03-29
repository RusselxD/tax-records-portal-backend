package com.taxrecordsportal.tax_records_portal_backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ){
        Map<String, String> fieldErrors = new HashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String paramName = exception.getName();

        String message;
        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            String acceptedValues = String.join(", ", java.util.Arrays.stream(exception.getRequiredType().getEnumConstants())
                    .map(Object::toString).toArray(String[]::new));
            message = "Invalid value for parameter '%s'. Accepted values: [%s]".formatted(paramName, acceptedValues);
        } else {
            String expectedType = exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "unknown";
            message = "Invalid value for parameter '%s'. Expected type: %s".formatted(paramName, expectedType);
        }

        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        Map<String, String> error = Map.of("message", "This record was modified by another user. Please refresh and try again.");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        Map<String, String> error = Map.of("message", exception.getReason() != null ? exception.getReason() : "An error occurred");
        return new ResponseEntity<>(error, exception.getStatusCode());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAuthorizationDenied(AuthorizationDeniedException exception) {
        return new ResponseEntity<>(Map.of("message", "Access denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return new ResponseEntity<>(Map.of("message", "Access denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return new ResponseEntity<>(Map.of("message", "Method '%s' is not supported for this endpoint".formatted(exception.getMethod())), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException exception) {
        return new ResponseEntity<>(Map.of("message", "Required parameter '%s' is missing".formatted(exception.getParameterName())), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return new ResponseEntity<>(Map.of("message", "Malformed request body"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return new ResponseEntity<>(Map.of("message", "File size exceeds the allowed limit"), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandler(NoHandlerFoundException exception) {
        return new ResponseEntity<>(Map.of("message", "Endpoint not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnect(AsyncRequestNotUsableException exception) {
        log.debug("Client disconnected: {}", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception exception) {
        log.error("Unhandled exception", exception);
        Map<String, String> error = Map.of("message", "An internal error occurred");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
