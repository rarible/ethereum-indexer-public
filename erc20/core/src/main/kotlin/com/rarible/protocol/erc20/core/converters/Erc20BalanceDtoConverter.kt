package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20BalanceDto
import com.rarible.protocol.erc20.core.model.Erc20Balance

object Erc20BalanceDtoConverter {

    fun convert(source: Erc20Balance): Erc20BalanceDto {
        return Erc20BalanceDto(
            contract = source.token,
            owner = source.owner,
            balance = source.balance.value,
            createdAt = source.createdAt,
            lastUpdatedAt = source.lastUpdatedAt,
            blockNumber = source.blockNumber
        )
    }
}
