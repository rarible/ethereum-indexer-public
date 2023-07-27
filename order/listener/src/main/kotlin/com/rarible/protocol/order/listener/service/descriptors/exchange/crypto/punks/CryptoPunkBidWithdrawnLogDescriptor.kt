package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidWithdrawnEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class CryptoPunkBidWithdrawnLogDescriptor(
    contractsProvider: ContractsProvider,
) : ExchangeSubscriber<OrderCancel>(
    name = "punk_bid_withdrawn",
    topic = PunkBidWithdrawnEvent.id(),
    contracts = contractsProvider.cryptoPunks()
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OrderCancel> {
        val bidWithdrawnEvent = PunkBidWithdrawnEvent.apply(log)
        val punkIndex = EthUInt256(bidWithdrawnEvent.punkIndex())
        val bidderAddress = bidWithdrawnEvent.fromAddress()
        val marketAddress = log.address()
        val orderHash = Order.hashKey(
            maker = bidderAddress,
            makeAssetType = EthAssetType,
            takeAssetType = CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)),
            salt = CRYPTO_PUNKS_SALT.value
        )
        return listOf(
            OrderCancel(
                hash = orderHash,
                maker = bidderAddress,
                make = Asset(EthAssetType, EthUInt256.ZERO),
                take = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)), EthUInt256.ONE),
                date = timestamp,
                source = HistorySource.CRYPTO_PUNKS
            )
        )
    }
}
