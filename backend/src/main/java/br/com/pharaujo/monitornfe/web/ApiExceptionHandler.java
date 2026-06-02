package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.BadRequestException;
import br.com.pharaujo.monitornfe.service.ResourceNotFoundException;
import br.com.pharaujo.monitornfe.web.dto.ApiErrorResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            List<String> details = validationException.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
            return build(HttpStatus.BAD_REQUEST, details);
        }
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String detail) {
        return build(status, List.of(detail));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, List<String> details) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            details
        ));
    }
}
