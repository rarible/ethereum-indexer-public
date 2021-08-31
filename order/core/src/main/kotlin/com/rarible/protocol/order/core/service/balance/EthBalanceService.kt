package com.rarible.protocol.order.core.service.balance

import com.rarible.ethereum.domain.EthUInt256
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address

@Component
class EthBalanceService(
    private val ethereum: MonoEthereum
) {
    suspend fun getBalance(address: Address): EthUInt256 =
        EthUInt256(ethereum.ethGetBalance(address, "latest").awaitSingle())
}