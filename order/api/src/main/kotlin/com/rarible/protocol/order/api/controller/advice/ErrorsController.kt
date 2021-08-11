package com.rarible.protocol.order.api.controller.advice

import com.rarible.ethereum.sign.service.CheckSignatureException
import com.rarible.protocol.dto.OrderIndexerApiErrorDto
import com.rarible.protocol.order.api.exceptions.IndexerApiException
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

    @ExceptionHandler(CheckSignatureException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleSignatureException(ex: CheckSignatureException) = mono {
        logger.error("Can't check signature", ex)

        OrderIndexerApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = OrderIndexerApiErrorDto.Code.SERVER_ERROR,
            message = "Unable to check signature"
        )
    }

    @ExceptionHandler(IndexerApiException::class)
    fun handleIndexerApiException(ex: IndexerApiException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = OrderIndexerApiErrorDto(
            status = ex.status.value(),
            code = ex.code,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = OrderIndexerApiErrorDto(
            status = ex.status.value(),
            code = OrderIndexerApiErrorDto.Code.ABSENCE_OF_REQUIRED_FIELD,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        OrderIndexerApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = OrderIndexerApiErrorDto.Code.UNKNOWN,
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
