package com.shipmate.api.common;

import com.shipmate.api.auth.MerchantUser;
import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.operation.error.ErrorLogService;
import com.shipmate.api.operation.error.ErrorLogService.ErrorRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final ErrorLogService errorLogService;
    private final RequestAuthContext requestAuthContext;

    public ApiExceptionHandler(ErrorLogService errorLogService, RequestAuthContext requestAuthContext) {
        this.errorLogService = errorLogService;
        this.requestAuthContext = requestAuthContext;
    }

    @ExceptionHandler(ApiRequestException.class)
    ProblemDetail handleApiRequestException(ApiRequestException exception, HttpServletRequest request) {
        errorLogService.append(exception.code(), exception.status().value(), exception.getMessage(), exception, errorContext(request));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        detail.setTitle(exception.code());
        detail.setProperty("code", exception.code());
        return detail;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail handleValidationException(Exception exception, HttpServletRequest request) {
        errorLogService.append("SM-COM-0001", HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다.", exception, errorContext(request));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
        detail.setTitle("SM-COM-0001");
        detail.setProperty("code", "SM-COM-0001");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleException(Exception exception, HttpServletRequest request) {
        errorLogService.append("SM-COM-0002", HttpStatus.INTERNAL_SERVER_ERROR.value(), "예상하지 못한 서버 오류가 발생했습니다.", exception, errorContext(request));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "예상하지 못한 서버 오류가 발생했습니다.");
        detail.setTitle("SM-COM-0002");
        detail.setProperty("code", "SM-COM-0002");
        return detail;
    }

    private ErrorRequestContext errorContext(HttpServletRequest request) {
        MerchantUser user = requestAuthContext.currentUser().orElse(null);
        return new ErrorRequestContext(
                request.getMethod(),
                request.getRequestURI(),
                user == null ? null : user.merchantId(),
                user == null ? null : user.merchantUserId()
        );
    }
}
