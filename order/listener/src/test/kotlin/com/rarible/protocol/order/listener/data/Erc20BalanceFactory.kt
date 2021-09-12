package com.rarible.protocol.order.listener.data

import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger

fun createErc20BalanceDto(): Erc20DecimalBalanceDto {
    return Erc20DecimalBalanceDto(
        owner = AddressFactory.create(),
        contract = AddressFactory.create(),
        balance = BigInteger.TEN,
        decimalBalance = BigDecimal.TEN
    )
}
