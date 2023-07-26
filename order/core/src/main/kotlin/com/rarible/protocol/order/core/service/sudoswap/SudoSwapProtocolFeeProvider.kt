package com.rarible.protocol.order.core.service.sudoswap

import com.rarible.protocol.contracts.exchange.sudoswap.v1.factory.LSSVMPairFactoryV1
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.math.BigInteger

@Component
class SudoSwapProtocolFeeProvider(ethereum: MonoEthereum) {
    private val sender = ReadOnlyMonoTransactionSender(ethereum, Address.ZERO())

    suspend fun getProtocolFeeMultiplier(factory: Address): BigInteger {
        return LSSVMPairFactoryV1(factory, sender).protocolFeeMultiplier().call().awaitFirst()
    }
}
