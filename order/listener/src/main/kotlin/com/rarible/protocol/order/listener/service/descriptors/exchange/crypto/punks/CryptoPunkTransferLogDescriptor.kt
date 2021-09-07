package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkTransferEvent
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
class CryptoPunkTransferLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkTransferEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderExchangeHistory> {
        val transferEvent = PunkTransferEvent.apply(log)
        val punkIndex = EthUInt256(transferEvent.punkIndex())
        val sellerAddress = transferEvent.from()
        val newOwnerAddress = transferEvent.to()
        val marketAddress = log.address()
        val cryptoPunksAssetType = CryptoPunksAssetType(marketAddress, punkIndex.value.toInt())
        val make = Asset(cryptoPunksAssetType, EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256.ZERO /* Price = 0 */)
        val makeOrder = OrderVersion(
            maker = sellerAddress,
            taker = newOwnerAddress,
            make = make,
            take = take,
            type = OrderType.CRYPTO_PUNKS,
            data = OrderCryptoPunksData,
            createdAt = date,
            platform = Platform.CRYPTO_PUNKS,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )
        val takeOrder = OrderVersion(
            maker = newOwnerAddress,
            taker = sellerAddress,
            make = take,
            take = make,
            type = OrderType.CRYPTO_PUNKS,
            data = OrderCryptoPunksData,
            createdAt = date,
            platform = Platform.CRYPTO_PUNKS,
            salt = CRYPTO_PUNKS_SALT,
            start = null,
            end = null,
            signature = null,
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null
        )
        return listOf(
            OnChainOrder(makeOrder),
            OnChainOrder(takeOrder),
            OrderSideMatch(
                hash = makeOrder.hash,
                counterHash = takeOrder.hash,
                side = OrderSide.LEFT,
                maker = sellerAddress,
                taker = newOwnerAddress,
                make = make,
                take = take,
                source = HistorySource.CRYPTO_PUNKS,
                date = date,
                fill = EthUInt256.ZERO, // Price = 0
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                externalOrderExecutedOnRarible = null
            ),
            OrderSideMatch(
                hash = takeOrder.hash,
                counterHash = makeOrder.hash,
                side = OrderSide.RIGHT,
                maker = newOwnerAddress,
                taker = sellerAddress,
                make = take,
                take = make,
                source = HistorySource.CRYPTO_PUNKS,
                date = date,
                fill = EthUInt256.ONE,
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                externalOrderExecutedOnRarible = null
            )
        )
    }
}