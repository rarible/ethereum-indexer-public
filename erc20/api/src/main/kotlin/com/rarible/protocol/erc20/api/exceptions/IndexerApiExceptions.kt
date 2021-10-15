package com.rarible.protocol.erc20.api.exceptions

import com.rarible.protocol.dto.Erc20IndexerApiErrorDto
import org.springframework.http.HttpStatus
import scalether.domain.Address

sealed class IndexerApiException(
    message: String,
    val status: HttpStatus,
    val code: Erc20IndexerApiErrorDto.Code
) : Exception(message)

class TokenNotFoundException(token: Address) : IndexerApiException(
    message = "Token $token not found",
    status = HttpStatus.NOT_FOUND,
    code = Erc20IndexerApiErrorDto.Code.TOKEN_NOT_FOUND
)
