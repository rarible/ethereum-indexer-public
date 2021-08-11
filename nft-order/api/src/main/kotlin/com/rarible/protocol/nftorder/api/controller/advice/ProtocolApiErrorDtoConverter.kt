package com.rarible.protocol.nftorder.api.controller.advice

import com.rarible.protocol.dto.NftIndexerApiErrorDto
import com.rarible.protocol.dto.NftOrderApiErrorDto
import com.rarible.protocol.dto.OrderIndexerApiErrorDto
import com.rarible.protocol.dto.UnlockableApiErrorDto
import org.springframework.http.HttpStatus

object ProtocolApiErrorDtoConverter {

    fun convert(apiErrorBody: Any): NftOrderApiErrorDto {
        return when (apiErrorBody) {
            is NftIndexerApiErrorDto -> nft(apiErrorBody)
            is OrderIndexerApiErrorDto -> order(apiErrorBody)
            is UnlockableApiErrorDto -> unlockable(apiErrorBody)
            else -> unexpected(apiErrorBody)
        }
    }

    private fun nft(dto: NftIndexerApiErrorDto): NftOrderApiErrorDto {
        return NftOrderApiErrorDto(
            status = dto.status,
            code = NftOrderApiErrorDto.Code.NFT_API_ERROR,
            message = "Received error from NFT-API with code ${dto.status} and message: ${dto.message}"
        )
    }

    private fun order(dto: OrderIndexerApiErrorDto): NftOrderApiErrorDto {
        return NftOrderApiErrorDto(
            status = dto.status,
            code = NftOrderApiErrorDto.Code.ORDER_API_ERROR,
            message = "Received error from Order-API with code ${dto.status} and message: ${dto.message}"
        )
    }

    private fun unlockable(dto: UnlockableApiErrorDto): NftOrderApiErrorDto {
        return NftOrderApiErrorDto(
            status = dto.status,
            code = NftOrderApiErrorDto.Code.UNLOCKABLE_API_ERROR,
            message = "Received error from Unlockable-API with code ${dto.status} and message: ${dto.message}"
        )
    }

    private fun unexpected(dto: Any): NftOrderApiErrorDto {
        return NftOrderApiErrorDto(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = NftOrderApiErrorDto.Code.UNEXPECTED_API_ERROR,
            message = "Received unexpected error from Protocol-API client: ${dto}"
        )
    }

}