package com.thoughtworks.agents.ohs.common;

import com.thoughtworks.agents.application.exception.BusinessException;
import com.thoughtworks.agents.domain.exception.GitHubApiException;
import com.thoughtworks.agents.domain.exception.GitHubMergeConflictException;
import com.thoughtworks.agents.domain.exception.GitHubNotAuthenticatedException;
import com.thoughtworks.agents.domain.exception.IllegalCCSessionStateException;
import com.thoughtworks.agents.domain.exception.IllegalConversationStateException;
import com.thoughtworks.agents.domain.exception.IllegalDevTaskStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.fail(400, message);
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.fail(400, ex.getMessage());
    }

    @ExceptionHandler(IllegalCCSessionStateException.class)
    public Result<Void> handleIllegalCCSessionStateException(IllegalCCSessionStateException ex) {
        return Result.fail(409, ex.getMessage());
    }

    @ExceptionHandler(IllegalConversationStateException.class)
    public Result<Void> handleIllegalConversationStateException(IllegalConversationStateException ex) {
        return Result.fail(409, ex.getMessage());
    }

    @ExceptionHandler(IllegalDevTaskStateException.class)
    public Result<Void> handleIllegalDevTaskStateException(IllegalDevTaskStateException ex) {
        return Result.fail(409, ex.getMessage());
    }

    @ExceptionHandler(GitHubNotAuthenticatedException.class)
    public Result<Void> handleGitHubNotAuthenticatedException(GitHubNotAuthenticatedException ex) {
        return Result.fail(401, ex.getMessage());
    }

    @ExceptionHandler(GitHubMergeConflictException.class)
    public Result<Void> handleGitHubMergeConflictException(GitHubMergeConflictException ex) {
        return Result.fail(409, ex.getMessage());
    }

    @ExceptionHandler(GitHubApiException.class)
    public Result<Void> handleGitHubApiException(GitHubApiException ex) {
        return Result.fail(502, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(500, "服务器内部错误");
    }
}
