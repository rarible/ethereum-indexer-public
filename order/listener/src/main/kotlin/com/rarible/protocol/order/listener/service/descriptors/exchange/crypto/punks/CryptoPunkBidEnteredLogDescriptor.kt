package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidEnteredEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class CryptoPunkBidEnteredLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val priceUpdateService: PriceUpdateService
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBidEnteredEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderExchangeHistory> {
        val punkBidEnteredEvent = PunkBidEnteredEvent.apply(log)
        val bidPrice = punkBidEnteredEvent.value()
        val punkIndex = punkBidEnteredEvent.punkIndex()
        val bidderAddress = punkBidEnteredEvent.fromAddress()
        val marketAddress = log.address()
        return listOf(
            OnChainOrder(
                OrderVersion(
                    maker = bidderAddress,
                    taker = null,
                    make = Asset(EthAssetType, EthUInt256(bidPrice)),
                    take = Asset(CryptoPunksAssetType(marketAddress, EthUInt256.of(punkIndex)), EthUInt256.ONE),
                    type = OrderType.CRYPTO_PUNKS,
                    salt = CRYPTO_PUNKS_SALT,
                    start = null,
                    end = null,
                    data = OrderCryptoPunksData,
                    signature = null,
                    createdAt = date,
                    platform = Platform.CRYPTO_PUNKS,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeUsd = null,
                    takeUsd = null
                ).run { priceUpdateService.withUpdatedUsdPrices(this) }
            )
        )
    }
}
