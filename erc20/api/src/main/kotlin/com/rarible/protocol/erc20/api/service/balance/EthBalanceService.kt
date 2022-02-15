package com.rarible.protocol.erc20.api.service.balance

import com.rarible.protocol.dto.EthBalanceDto
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address

@Component
class EthBalanceService(
    private val ethereum: MonoEthereum
) {

    suspend fun getBalance(address: Address): EthBalanceDto {
        val balance = ethereum.ethGetBalance(address, "latest").awaitSingle()
        return EthBalanceDto(
            owner = address,
            balance = balance,
            // 18 is the ETH scale (1 ETH == 10^18 units returned from MonoEthereum)
            decimalBalance = balance.toBigDecimal(18)
        )
    }
}