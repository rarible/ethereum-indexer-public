package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.wyvern.NonceIncrementedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.NonceSubscriber
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

// @Service //TODO: Activate after move to a new scanner
@CaptureSpan(type = SpanType.EVENT)
class WyvernExchangeChangeNoneDescriptor(
    contractsProvider: ContractsProvider,
    private val properties: OrderIndexerProperties
) : NonceSubscriber(
    name = "os_nonce_incremented",
    topic = NonceIncrementedEvent.id(),
    contracts = contractsProvider.openSea()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<ChangeNonceHistory> {
        val event = NonceIncrementedEvent.apply(log)
        return listOf(
            ChangeNonceHistory(
                maker = event.maker(),
                newNonce = EthUInt256.of(event.newNonce() + BigInteger.valueOf(properties.openSeaNonceIncrement)),
                date = timestamp,
                source = HistorySource.OPEN_SEA
            )
        )
    }
}
