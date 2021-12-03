package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkOfferedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class CryptoPunkOfferedLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
    private val priceUpdateService: PriceUpdateService,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkOfferedEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderExchangeHistory> {
        val punkOfferedEvent = PunkOfferedEvent.apply(log)
        val grantedBuyer = punkOfferedEvent.toAddress().takeUnless { it == Address.ZERO() }
        val punkIndex = EthUInt256(punkOfferedEvent.punkIndex())
        val previousSellOrderCancel = listOfNotNull(
            CryptoPunkBoughtLogDescriptor.getCancelOfSellOrder(
                exchangeHistoryRepository = exchangeHistoryRepository,
                marketAddress = log.address(),
                blockDate = date,
                blockNumber = log.blockNumber().toLong(),
                logIndex = log.logIndex().toInt(),
                punkIndex = punkIndex.value
            )
        )
        if (grantedBuyer == transferProxyAddresses.cryptoPunksTransferProxy) {
            return previousSellOrderCancel
        }
/*
        We do not ignore SELL orders specific to a concrete buyer, because we want to track the whole CryptoPunks order history.

        if (grantedBuyer != Address.ZERO()) {
            logger.info(
                "Ignore offering of punk #${punkOfferedEvent.punkIndex()} to a concrete address " +
                        "($grantedBuyer), listen to only public offerings."
            )
            return emptyList()
        }
*/
        val minPrice = punkOfferedEvent.minValue()
        val marketAddress = log.address()
        val sellerAddress = transaction.from()
        val make = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(minPrice))
        val usdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        val sellOrderHash = Order.hashKey(sellerAddress, make.type, take.type, CRYPTO_PUNKS_SALT.value)
        return previousSellOrderCancel + listOf(
            OnChainOrder(
                hash = sellOrderHash,
                maker = sellerAddress,
                taker = grantedBuyer,
                make = make,
                take = take,
                orderType = OrderType.CRYPTO_PUNKS,

                salt = CRYPTO_PUNKS_SALT,
                start = null,
                end = null,
                data = OrderCryptoPunksData,
                signature = null,
                createdAt = date,
                platform = Platform.CRYPTO_PUNKS,
                priceUsd = usdValue?.makePriceUsd ?: usdValue?.takePriceUsd
            )
        )
    }
}
