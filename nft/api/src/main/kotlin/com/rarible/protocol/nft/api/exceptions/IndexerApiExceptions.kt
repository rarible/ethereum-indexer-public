package com.rarible.protocol.nft.api.exceptions

import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import org.springframework.http.HttpStatus

sealed class NftIndexerApiException(
    message: String,
    val status: HttpStatus,
    val data: Any
) : Exception(message)

class EntityNotFoundApiException(type: String, id: Any) : NftIndexerApiException(
    message = getNotFoundMessage(type, id),
    status = HttpStatus.NOT_FOUND,
    data = EthereumApiErrorEntityNotFoundDto(
        message = getNotFoundMessage(type, id),
        code = EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND
    )
)

class ValidationApiException(message: String) : NftIndexerApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    data = EthereumApiErrorBadRequestDto(
        message = message,
        code = EthereumApiErrorBadRequestDto.Code.VALIDATION
    )
)

private fun getNotFoundMessage(type: String, id: Any): String {
    return "$type with id $id not found"
}
