package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidEnteredEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class CryptoPunkBidEnteredLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val priceUpdateService: PriceUpdateService,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBidEnteredEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderExchangeHistory> {
        val punkBidEnteredEvent = PunkBidEnteredEvent.apply(log)
        val bidPrice = punkBidEnteredEvent.value()
        val punkIndex = punkBidEnteredEvent.punkIndex()
        val bidderAddress = punkBidEnteredEvent.fromAddress()
        val marketAddress = log.address()
        val make = Asset(EthAssetType, EthUInt256(bidPrice))
        val take = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex)), EthUInt256.ONE)
        val usdValue = priceUpdateService.getAssetsUsdValue(make, take, nowMillis())
        val cancelOfPreviousBid = getCancelOfPreviousBid(exchangeHistoryRepository, marketAddress, date, punkIndex)
        return listOfNotNull(
            cancelOfPreviousBid,
            OnChainOrder(
                maker = bidderAddress,
                taker = null,
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
                priceUsd = usdValue?.makePriceUsd ?: usdValue?.takePriceUsd,
                hash = Order.hashKey(bidderAddress, make.type, take.type, CRYPTO_PUNKS_SALT.value)
            )
        )
    }

    companion object {
        /**
         * Find the previous bid for the same punk. It must be cancelled if the new bid's price is higher.
         */
        suspend fun getCancelOfPreviousBid(
            exchangeHistoryRepository: ExchangeHistoryRepository,
            marketAddress: Address,
            blockDate: Instant,
            punkIndex: BigInteger
        ): OrderCancel? {
            val lastBidEvent = exchangeHistoryRepository
                .findBidEventsByItem(marketAddress, EthUInt256(punkIndex)).collectList()
                .awaitFirst().lastOrNull()?.data
            // If the latest exchange event for this punk is OnChainOrder (and not OrderCancel nor OrderSideMatch)
            //  it means that there was a previous bid from another user for the same punk with smaller price.
            //  That bid must be cancelled.
            if (lastBidEvent is OnChainOrder) {
                return OrderCancel(
                    hash = lastBidEvent.hash,
                    maker = lastBidEvent.maker,
                    make = lastBidEvent.make,
                    take = lastBidEvent.take,
                    date = blockDate,
                    source = HistorySource.CRYPTO_PUNKS
                )
            }
            return null
        }

    }
}
