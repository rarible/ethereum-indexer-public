package com.rarible.protocol.erc20.api.converter

import com.rarible.core.contract.model.Contract
import com.rarible.protocol.dto.Erc20TokenDto

object Erc20TokenDtoConverter {

    fun convert(source: Contract): Erc20TokenDto {
        return Erc20TokenDto(
            id = source.id,
            name = source.name,
            symbol = source.symbol
        )
    }
}
