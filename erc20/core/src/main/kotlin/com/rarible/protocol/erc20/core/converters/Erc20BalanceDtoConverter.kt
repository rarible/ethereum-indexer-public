package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.erc20.core.model.Erc20Balance
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object Erc20BalanceDtoConverter : Converter<Erc20Balance, Erc20BalanceDto> {
    override fun convert(source: Erc20Balance): Erc20BalanceDto {
        return Erc20BalanceDto(
            contract = source.token,
            owner = source.owner,
            balance = source.balance.value,
            createdAt = source.createdAt,
            lastUpdatedAt = source.lastUpdatedAt
        )
    }
}

