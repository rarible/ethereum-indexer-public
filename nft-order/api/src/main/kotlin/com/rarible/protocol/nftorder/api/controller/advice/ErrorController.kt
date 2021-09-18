package com.rarible.protocol.nftorder.api.controller.advice

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorServerErrorDto
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.nftorder.api.controller"])
class ErrorController {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        logger.warn("Incorrect input data in request: {}", ex.message)
        val error = EthereumApiErrorBadRequestDto(
            code = EthereumApiErrorBadRequestDto.Code.BAD_REQUEST,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: ""
        )
        // For ServerWebInputException status is always 400
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(WebClientResponseProxyException::class)
    fun handleWebClientResponseException(ex: WebClientResponseProxyException) = mono {
        logger.warn("Exception during Protocol-API request to another service: {}; response: {}", ex.message, ex.data)
        ResponseEntity.status(ex.statusCode).body(ex.data)
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
}