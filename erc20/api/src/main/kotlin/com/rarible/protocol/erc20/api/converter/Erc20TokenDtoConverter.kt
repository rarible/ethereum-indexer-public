package com.rarible.protocol.erc20.api.converter

import com.rarible.core.contract.model.Contract
import com.rarible.protocol.dto.Erc20TokenDto
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object Erc20TokenDtoConverter : Converter<Contract, Erc20TokenDto> {
    override fun convert(source: Contract): Erc20TokenDto {
        return Erc20TokenDto(
            id = source.id,
            name = source.name,
            symbol = source.symbol
        )
    }
}