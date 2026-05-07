package com.quran.web.exception;

import com.quran.web.dto.response.ApiResponse;
import com.quran.web.dto.response.ErrorResponse;
import com.quran.web.dto.response.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Static resource not found: {}", ex.getResourcePath());
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.setViewName("error/404");
        modelAndView.addObject("message", "The resource you are looking for does not exist.");
        return modelAndView;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.error("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(
            InvalidRequestException ex) {

        log.error("Invalid request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            org.springframework.web.context.request.WebRequest request) {

        log.error("Validation error: {}", ex.getMessage());

        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ValidationError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
        }

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        modelAndView.setViewName("error/500");
        modelAndView.addObject("message", "An unexpected error occurred. Please try again later.");
        return modelAndView;
    }
}
