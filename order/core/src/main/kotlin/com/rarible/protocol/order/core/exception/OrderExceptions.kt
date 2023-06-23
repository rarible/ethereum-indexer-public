package com.rarible.protocol.order.core.exception

import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.EthereumOrderDataApiErrorDto
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import org.springframework.http.HttpStatus

sealed class OrderIndexerException(
    message: String,
    val status: HttpStatus,
    val data: Any
) : Exception(message)

class EntityNotFoundApiException(type: String, id: Any) : OrderIndexerException(
    message = getNotFoundMessage(type, id),
    status = HttpStatus.NOT_FOUND,
    data = EthereumApiErrorEntityNotFoundDto(
        message = getNotFoundMessage(type, id),
        code = EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND
    )
)

class ValidationApiException(message: String) : OrderIndexerException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    data = EthereumApiErrorBadRequestDto(
        message = message,
        code = EthereumApiErrorBadRequestDto.Code.VALIDATION
    )
)

class OrderDataException(message: String) : OrderIndexerException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    data = EthereumOrderDataApiErrorDto(
        message = message,
        code = EthereumOrderDataApiErrorDto.Code.INCORRECT_ORDER_DATA
    )
)

class OrderUpdateException(message: String, code: EthereumOrderUpdateApiErrorDto.Code) : OrderIndexerException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    data = EthereumOrderUpdateApiErrorDto(
        message = message,
        code = code
    )
)

private fun getNotFoundMessage(type: String, id: Any): String {
    return "$type with id $id not found"
}
