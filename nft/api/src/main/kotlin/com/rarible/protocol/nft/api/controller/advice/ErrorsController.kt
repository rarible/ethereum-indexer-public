package com.rarible.protocol.nft.api.controller.advice

import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.nft.api.exceptions.IndexerApiException
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.nft.api.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(IndexerApiException::class)
    fun handleIndexerApiException(ex: IndexerApiException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = NftIndexerApiErrorDto(
            status = ex.status.value(),
            code = ex.code,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = NftIndexerApiErrorDto(
            status = ex.status.value(),
            code = NftIndexerApiErrorDto.Code.BAD_REQUEST,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        NftIndexerApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = NftIndexerApiErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }

    private fun logWithNecessaryLevel(status: HttpStatus, ex: Exception, message: String = "") {
        when {
            status == HttpStatus.NOT_FOUND -> logger.warn(message)
            status.is5xxServerError -> logger.error(message, ex)
            else -> logger.warn(message, ex)
        }
    }
}
