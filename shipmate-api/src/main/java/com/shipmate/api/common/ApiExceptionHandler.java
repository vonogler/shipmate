package com.shipmate.api.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiRequestException.class)
    ProblemDetail handleApiRequestException(ApiRequestException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        detail.setTitle(exception.code());
        detail.setProperty("code", exception.code());
        return detail;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail handleValidationException(Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request.");
        detail.setTitle("SM-COM-0001");
        detail.setProperty("code", "SM-COM-0001");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleException(Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
        detail.setTitle("SM-COM-0002");
        detail.setProperty("code", "SM-COM-0002");
        return detail;
    }
}
