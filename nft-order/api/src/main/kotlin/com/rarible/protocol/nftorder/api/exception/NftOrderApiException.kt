package com.rarible.protocol.nftorder.api.exception

import com.rarible.protocol.dto.NftOrderApiErrorDto
import org.springframework.http.HttpStatus

sealed class NftOrderApiException(
    message: String,
    val status: HttpStatus,
    val code: NftOrderApiErrorDto.Code
) : Exception(message)

class InvalidParameterException(message: String) : NftOrderApiException(
    message = message,
    status = HttpStatus.BAD_REQUEST,
    code = NftOrderApiErrorDto.Code.INVALID_ARGUMENT
)