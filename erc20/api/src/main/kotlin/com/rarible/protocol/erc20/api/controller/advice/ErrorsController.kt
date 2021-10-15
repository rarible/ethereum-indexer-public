package com.rarible.protocol.erc20.api.controller.advice

import com.rarible.protocol.dto.ArgumentFormatException
import com.rarible.protocol.dto.Erc20IndexerApiErrorDto.Code
import com.rarible.protocol.dto.Erc20IndexerApiErrorDto
import com.rarible.protocol.erc20.api.exceptions.IndexerApiException
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.erc20.api.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(
        value = [
            ServerWebInputException::class,
            ArgumentFormatException::class
        ]
    )
    fun handleServerWebInputException(ex: Exception) = mono {
        val status = HttpStatus.BAD_REQUEST

        val error = Erc20IndexerApiErrorDto(
            status = HttpStatus.BAD_REQUEST.value(),
            code = Code.VALIDATION,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: ""
        )
        ResponseEntity.status(status).body(error)
    }

    @ExceptionHandler(IndexerApiException::class)
    fun handleIndexerApiException(ex: IndexerApiException) = mono {
        val error = Erc20IndexerApiErrorDto(
            status = ex.status.value(),
            code = ex.code,
            message = ex.message ?: "Missing message in error ${ex.javaClass.name}"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        Erc20IndexerApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }
}
