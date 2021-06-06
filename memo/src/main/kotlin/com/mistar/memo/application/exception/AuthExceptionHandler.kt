package com.mistar.memo.application.exception

import com.mistar.memo.core.response.ErrorResponse
import com.mistar.memo.domain.exception.*
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleUserAlreadyExists(exception: UserAlreadyExistsException): ErrorResponse {
        return ErrorResponse(HttpStatus.CONFLICT, "User-001", exception.message!!)
    }

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFound(exception: UserNotFoundException): ErrorResponse {
        return ErrorResponse(HttpStatus.NOT_FOUND, "User-002", exception.message!!)
    }

    @ExceptionHandler(InvalidPasswordException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleInvalidPassword(exception: InvalidPasswordException): ErrorResponse {
        return ErrorResponse(HttpStatus.CONFLICT, "User-003", exception.message!!)
    }

    @ExceptionHandler(UserNotSignedInException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotSignedIn(exception: UserNotSignedInException): ErrorResponse {
        return ErrorResponse(HttpStatus.NOT_FOUND, "User-004", exception.message!!)
    }

    @ExceptionHandler(RefreshTokenNotMatchedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleRefreshTokenNotMatched(exception: RefreshTokenNotMatchedException): ErrorResponse {
        return ErrorResponse(HttpStatus.CONFLICT, "User-005", exception.message!!)
    }
}