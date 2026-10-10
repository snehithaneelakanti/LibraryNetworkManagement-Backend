
package com.librarynetwork.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException exception) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid email or password"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                )
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Validation Failed");
        body.put("message", "Please correct the invalid fields");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more supplied values are invalid"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(
            HttpMessageNotReadableException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "The request body is missing or contains invalid data"
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException exception) {

        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

        String message = exception.getReason() == null
                ? status.getReasonPhrase()
                : exception.getReason();

        return error(status, status.getReasonPhrase(), message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException exception) {
        return error(
                HttpStatus.CONFLICT,
                "Conflict",
                "The operation conflicts with existing database records"
        );
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String error,
            String message) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
