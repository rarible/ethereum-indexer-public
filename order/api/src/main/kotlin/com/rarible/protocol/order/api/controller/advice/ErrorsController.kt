package com.rarible.protocol.order.api.controller.advice

import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorServerErrorDto
import com.rarible.protocol.order.api.exceptions.OrderIndexerApiException
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.order.api.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(OrderIndexerApiException::class)
    fun handleIndexerApiException(ex: OrderIndexerApiException) = mono {
        ResponseEntity.status(ex.status).body(ex.data)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        // For ServerWebInputException status is always 400
        val error = EthereumApiErrorBadRequestDto(
            code = EthereumApiErrorBadRequestDto.Code.BAD_REQUEST,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: MISSING_MESSAGE
        )
        logger.warn("Web input error: {}", error.message)
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)
        EthereumApiErrorServerErrorDto(
            code = EthereumApiErrorServerErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }

    companion object {
        const val MISSING_MESSAGE = "Missing message in error"
    }
}
