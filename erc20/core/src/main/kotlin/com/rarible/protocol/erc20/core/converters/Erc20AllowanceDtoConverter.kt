package com.rarible.protocol.erc20.core.converters

import com.rarible.protocol.dto.Erc20AllowanceDto
import com.rarible.protocol.erc20.core.model.Erc20Allowance

object Erc20AllowanceDtoConverter {

    fun convert(source: Erc20Allowance): Erc20AllowanceDto {
        return Erc20AllowanceDto(
            contract = source.token,
            owner = source.owner,
            allowance = source.allowance.value,
            createdAt = source.createdAt,
            lastUpdatedAt = source.lastUpdatedAt,
        )
    }
}

