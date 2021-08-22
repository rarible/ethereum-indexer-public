package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidEnteredEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class CryptoPunkBidEnteredLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : ItemExchangeHistoryLogEventDescriptor<OrderNew> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBidEnteredEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderNew> {
        val punkBidEnteredEvent = PunkBidEnteredEvent.apply(log)
        val bidPrice = punkBidEnteredEvent.value()
        val punkIndex = EthUInt256(punkBidEnteredEvent.punkIndex())
        val bidderAddress = punkBidEnteredEvent.fromAddress()
        val marketAddress = log.address()
        return listOf(
            OrderNew(
                Order(
                    maker = bidderAddress,
                    taker = null,
                    make = Asset(EthAssetType, EthUInt256(bidPrice)),
                    take = Asset(CryptoPunksAssetType(marketAddress, punkIndex.value.toInt()), EthUInt256.ONE),
                    type = OrderType.CRYPTO_PUNKS,
                    fill = EthUInt256.ZERO,
                    cancelled = false,

                    makeStock = EthUInt256.ZERO, //TODO[punk]: not sure meaning of this field.
                    salt = EthUInt256.ZERO, //TODO[punk]: not sure.
                    start = null,
                    end = null,
                    data = OrderCryptoPunksData(marketAddress),
                    signature = null, //TODO[punk]: not sure.
                    createdAt = date,
                    lastUpdateAt = date,
                    platform = Platform.CRYPTO_PUNKS
                )
            )
        )
    }
}