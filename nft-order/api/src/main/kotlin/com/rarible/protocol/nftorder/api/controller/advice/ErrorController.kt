package com.rarible.protocol.nftorder.api.controller.advice

import com.rarible.protocol.client.exception.ProtocolApiResponseException
import com.rarible.protocol.dto.NftOrderApiErrorDto
import com.rarible.protocol.nftorder.api.exception.NftOrderApiException
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.nftorder.api.controller"])
class ErrorController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // TODO - not used yet
    @ExceptionHandler(NftOrderApiException::class)
    fun handleIndexerApiException(ex: NftOrderApiException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = NftOrderApiErrorDto(
            status = ex.status.value(),
            code = ex.code,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException) = mono {
        logWithNecessaryLevel(ex.status, ex, "Indexer api error while handle request")

        val error = NftOrderApiErrorDto(
            status = ex.status.value(),
            code = NftOrderApiErrorDto.Code.ABSENCE_OF_REQUIRED_FIELD,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(ex.status).body(error)
    }

    @ExceptionHandler(ProtocolApiResponseException::class)
    fun handleProtocolApiException(ex: ProtocolApiResponseException) = mono {
        logger.error("Exception during Protocol-API request to another service:", ex)
        val error = ProtocolApiErrorDtoConverter.convert(ex.responseObject)
        ResponseEntity.status(error.status).body(error)
    }

    @ExceptionHandler(WebClientResponseException::class)
    fun handleProtocolApiClientException(ex: WebClientResponseException) = mono {
        logger.error("Unexpected client exception during Protocol-API request to another service:", ex)
        val error = NftOrderApiErrorDto(
            status = ex.rawStatusCode,
            code = NftOrderApiErrorDto.Code.UNEXPECTED_API_ERROR,
            message = ex.message ?: "Missing message in error"
        )
        ResponseEntity.status(error.status).body(error)
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerException(ex: Throwable) = mono {
        logger.error("System error while handling request", ex)

        NftOrderApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = NftOrderApiErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Something went wrong"
        )
    }

    private fun logWithNecessaryLevel(status: HttpStatus, ex: Exception, message: String = "") {
        if (status.is5xxServerError) {
            logger.error(message, ex)
        } else {
            logger.warn(message, ex)
        }
    }
}