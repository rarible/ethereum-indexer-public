package com.rarible.protocol.unlockable.api.controller.advice

import com.rarible.protocol.dto.UnlockableApiErrorDto
import com.rarible.protocol.unlockable.api.exception.LockOperationException
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.rarible.protocol.unlockable.api.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(LockOperationException::class)
    fun handleIndexerApiException(ex: LockOperationException) = mono {
        val error = UnlockableApiErrorDto(
            status = HttpStatus.BAD_REQUEST.value(),
            code = ex.errorCode,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(error.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        UnlockableApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = UnlockableApiErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }
}
