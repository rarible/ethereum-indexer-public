package com.rarible.protocol.gateway.controller.advice

import com.rarible.protocol.dto.GatewayApiErrorDto
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.rarible.protocol.gateway.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        GatewayApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = GatewayApiErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }

    private suspend fun logWithNecessaryLevel(status: HttpStatus, ex: Exception, message: String = "") {
        if (status.is5xxServerError) {
            logger.error(message, ex)
        } else {
            logger.warn(message, ex)
        }
    }
}