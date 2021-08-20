package com.rarible.protocol.nft.api.controller.advice

import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.nft.api.exceptions.IndexerApiException
import com.rarible.protocol.nft.core.model.IncorrectItemFormat
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionFailedException
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
        logWithNecessaryLevel(ex.status, ex, INDEXER_API_ERROR)

        val error = NftIndexerApiErrorDto(
            status = ex.status.value(),
            code = ex.code,
            message = ex.message ?: MISSING_MESSAGE
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        logWithNecessaryLevel(ex.status, ex, INDEXER_API_ERROR)

        val error = NftIndexerApiErrorDto(
            status = ex.status.value(),
            code = NftIndexerApiErrorDto.Code.BAD_REQUEST,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: MISSING_MESSAGE
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ConversionFailedException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerConversionFailedException(ex: ConversionFailedException) = mono {
        when (ex.cause) {
            is IncorrectItemFormat -> {
                logWithNecessaryLevel(HttpStatus.BAD_REQUEST, ex, INDEXER_API_ERROR)
                NftIndexerApiErrorDto(
                    status = HttpStatus.BAD_REQUEST.value(),
                    code = NftIndexerApiErrorDto.Code.BAD_REQUEST,
                    message = ex.cause?.message ?: MISSING_MESSAGE
                )
            }
            else -> logError(ex)
        }
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logError(ex)
    }

    private fun logWithNecessaryLevel(status: HttpStatus, ex: Exception, message: String = "") {
        when {
            status == HttpStatus.NOT_FOUND -> logger.warn(message)
            status.is5xxServerError -> logger.error(message, ex)
            else -> logger.warn(message, ex)
        }
    }

    private fun logError(ex: Throwable) {
        logger.error("System error while handling request", ex)

        NftIndexerApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = NftIndexerApiErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }

    companion object {
        const val MISSING_MESSAGE = "Missing message in error"
        const val INDEXER_API_ERROR = "Indexer api error while handle request"
    }
}
