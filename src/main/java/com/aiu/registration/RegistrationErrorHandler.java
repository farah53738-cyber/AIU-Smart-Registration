package com.aiu.registration;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RegistrationErrorHandler {
  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public ResponseEntity<Map<String, String>> malformed(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Check the required fields, numbers and dates."));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> duplicate(Exception e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "message",
                "This change conflicts with an existing record or a required reference."));
  }

  // Return clear validation messages
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleExpected(ResponseStatusException ex) {

    String message = ex.getReason() == null ? "Request could not be completed." : ex.getReason();

    return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
  }

  // Hide internal database details
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<Map<String, String>> handleDatabase(DataAccessException ex) {

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("message", "Database operation failed. Please try again."));
  }
}
