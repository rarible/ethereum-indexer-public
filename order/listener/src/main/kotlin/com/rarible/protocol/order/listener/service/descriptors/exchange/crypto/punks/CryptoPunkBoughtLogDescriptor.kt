package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant

@Service
class CryptoPunkBoughtLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {
    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBoughtEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderExchangeHistory> {
        val punkBoughtEvent = PunkBoughtEvent.apply(log)
        val marketAddress = log.address()
        val cryptoPunksAssetType = CryptoPunksAssetType(marketAddress, punkBoughtEvent.punkIndex().toInt())
        val sellerAddress = punkBoughtEvent.fromAddress()
        val buyerAddress = punkBoughtEvent.toAddress()
        val orderSalt = BigInteger.ZERO // TODO[punk]: not sure.
        val hash = Order.hashKey(
            maker = sellerAddress,
            makeAssetType = cryptoPunksAssetType,
            takeAssetType = EthAssetType,
            salt = orderSalt
        )
        val counterHash = Order.hashKey(
            maker = buyerAddress,
            makeAssetType = EthAssetType,
            takeAssetType = cryptoPunksAssetType,
            salt = orderSalt
        )
        val make = Asset(cryptoPunksAssetType, EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkBoughtEvent.value()))
        return listOf(
            OrderNew(
                Order(
                    maker = buyerAddress,
                    taker = sellerAddress,
                    make = take,
                    take = make,
                    type = OrderType.CRYPTO_PUNKS,
                    fill = EthUInt256.ZERO,
                    cancelled = false,
                    data = OrderCryptoPunksData(marketAddress),
                    platform = Platform.CRYPTO_PUNKS,
                    //TODO[punk]: not sure about these values:
                    makeStock = EthUInt256.ZERO,
                    salt = EthUInt256(orderSalt),
                    start = null,
                    end = null,
                    signature = null,
                    createdAt = date,
                    lastUpdateAt = date,
                    pending = emptyList(),
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makeUsd = null,
                    takeUsd = null,
                    priceHistory = emptyList(),
                    externalOrderExecutedOnRarible = null
                )
            ),
            OrderSideMatch(
                hash = hash,
                counterHash = counterHash,
                side = OrderSide.LEFT,
                maker = sellerAddress,
                taker = buyerAddress,
                make = make,
                take = take,
                date = date,
                fill = take.value,
                //TODO[punk]: not sure about these values:
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                source = HistorySource.CRYPTO_PUNKS,
                externalOrderExecutedOnRarible = null
            ),
            OrderSideMatch(
                hash = counterHash,
                counterHash = hash,
                side = OrderSide.RIGHT,
                maker = buyerAddress,
                taker = sellerAddress,
                make = take,
                take = make,
                date = date,
                fill = EthUInt256.ONE,
                //TODO[punk]: not sure about these values:
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                source = HistorySource.CRYPTO_PUNKS,
                externalOrderExecutedOnRarible = null
            )
        )
    }
}