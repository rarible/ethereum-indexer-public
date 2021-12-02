package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.TransferEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CRYPTO_PUNKS_SALT
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.request.LogFilter
import scalether.domain.request.TopicFilter
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class CryptoPunkBoughtLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
    private val ethereum: MonoEthereum
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    private val logger = LoggerFactory.getLogger(CryptoPunkBoughtLogDescriptor::class.java)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBoughtEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderExchangeHistory> {
        val punkBoughtEvent = PunkBoughtEvent.apply(log)
        val marketAddress = log.address()
        val punkIndex = punkBoughtEvent.punkIndex()
        val cryptoPunksAssetType = CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex))
        val sellerAddress = punkBoughtEvent.fromAddress()
        val buyerAddress = getBuyerAddress(punkBoughtEvent)
        if (buyerAddress == transferProxyAddresses.cryptoPunksTransferProxy) {
            // We ignore "buy for 0ETH" from the owner to the transfer proxy events,
            // because this is the Exchange contract's implementation detail.
            return emptyList()
        }
        val calledFunctionSignature = getCalledFunctionSignature(punkBoughtEvent)
        val punkPrice = getPunkPrice(punkBoughtEvent, calledFunctionSignature, transaction)
        val externalOrderExecutedOnRarible = isExternalOrderExecutedOnRarible(transaction)
        val sellOrderHash = Order.hashKey(
            maker = sellerAddress,
            makeAssetType = cryptoPunksAssetType,
            takeAssetType = EthAssetType,
            salt = CRYPTO_PUNKS_SALT.value
        )
        val buyOrderHash = Order.hashKey(
            maker = buyerAddress,
            makeAssetType = EthAssetType,
            takeAssetType = cryptoPunksAssetType,
            salt = CRYPTO_PUNKS_SALT.value
        )
        val make = Asset(cryptoPunksAssetType, EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(punkPrice))

        val cancelSellOrder = if (calledFunctionSignature == CryptoPunksMarket.acceptBidForPunkSignature().name()) {
            getCancelOfSellOrder(
                exchangeHistoryRepository = exchangeHistoryRepository,
                marketAddress = marketAddress,
                blockDate = date,
                blockNumber = log.blockNumber().toLong(),
                logIndex = log.logIndex().toInt(),
                punkIndex = punkIndex
            )
        } else {
            null
        }

        // the left order is always a sell order; if we receive accept bid -> left adhoc = true
        val adhoc = calledFunctionSignature == CryptoPunksMarket.acceptBidForPunkSignature().name()
        // the right order is always a buy order; if we receive buy -> right adhoc = true
        val counterAdhoc = calledFunctionSignature == CryptoPunksMarket.buyPunkSignature().name()
        return listOfNotNull(
            cancelSellOrder,
            OrderSideMatch(
                hash = sellOrderHash,
                counterHash = buyOrderHash,
                side = OrderSide.LEFT,
                maker = sellerAddress,
                taker = buyerAddress,
                make = make,
                take = take,
                date = date,
                fill = take.value,
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                source = HistorySource.CRYPTO_PUNKS,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                adhoc = adhoc,
                counterAdhoc = counterAdhoc
            ),
            OrderSideMatch(
                hash = buyOrderHash,
                counterHash = sellOrderHash,
                side = OrderSide.RIGHT,
                maker = buyerAddress,
                taker = sellerAddress,
                make = take,
                take = make,
                date = date,
                fill = EthUInt256.ONE,
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = null,
                takeValue = null,
                source = HistorySource.CRYPTO_PUNKS,
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible,
                adhoc = counterAdhoc,
                counterAdhoc = adhoc
            )
        )
    }

    private fun isExternalOrderExecutedOnRarible(transaction: Transaction): Boolean {
        return transaction.input().prefixed().endsWith(Platform.CRYPTO_PUNKS.id.hex())
    }

    private fun getCalledFunctionSignature(punkBoughtEvent: PunkBoughtEvent): String {
        // Use bug https://github.com/larvalabs/cryptopunks/issues/19 to determine the called function.
        // If "toAddress" = 0, this is "acceptBidForPunk", otherwise this is "buyPunk".
        return if (punkBoughtEvent.toAddress() == Address.ZERO()) {
            CryptoPunksMarket.acceptBidForPunkSignature().name()
        } else {
            CryptoPunksMarket.buyPunkSignature().name()
        }
    }

    private fun getPunkPrice(
        punkBoughtEvent: PunkBoughtEvent,
        calledFunctionSignature: String,
        transaction: Transaction
    ): BigInteger {
        if (punkBoughtEvent.value() != BigInteger.ZERO || calledFunctionSignature != CryptoPunksMarket.acceptBidForPunkSignature()
                .name()
        ) {
            return punkBoughtEvent.value()
        }
        // Because of https://github.com/larvalabs/cryptopunks/issues/19 we cannot extract the correct "bid.value" for "acceptBidForPunk" function.
        // Thus, we try to guess the value from the "minPrice" parameter passed to transaction.
        // We consider that "minPrice" == "bid.value". Of course this may not be true:
        // 1) Seller might have set "minPrice = 0" when he saw the punk bid, which he was ready to accept.
        // 2) There might have been another bid with bigger "bid.value" appeared before the "acceptBidForPunk" transaction was accepted.
        val decodedInput = CryptoPunksMarket.acceptBidForPunkSignature().`in`().decode(
            transaction.input(),
            CryptoPunksMarket.acceptBidForPunkSignature().id().length()
        )
        return decodedInput.value()._2 // "minPrice" parameter.
    }

    private suspend fun getBuyerAddress(punkBoughtEvent: PunkBoughtEvent): Address {
        if (punkBoughtEvent.toAddress() != Address.ZERO()) {
            return punkBoughtEvent.toAddress()
        }
        /*
           Workaround https://github.com/larvalabs/cryptopunks/issues/19.
           We have to find "Transfer" event going before "PunkBought"
           from the same function in order to extract correct value for "toAddress".
        */
        val filter = LogFilter
            .apply(TopicFilter.simple(TransferEvent.id()))
            .address(exchangeContractAddresses.cryptoPunks)
            .blockHash(punkBoughtEvent.log().blockHash())
        val logs = try {
            ethereum.ethGetLogsJava(filter).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Unable to get logs for block ${punkBoughtEvent.log().blockHash()}", e)
            throw e
        }
        return logs.find {
            it.topics().head() == TransferEvent.id()
                    && it.transactionHash() == punkBoughtEvent.log().transactionHash()
                    && it.logIndex() == punkBoughtEvent.log().logIndex().minus(BigInteger.ONE)
        }
            ?.let { TransferEvent.apply(it) }
            ?.to()
            ?: punkBoughtEvent.toAddress()
    }

    companion object {
        suspend fun getCancelOfSellOrder(
            exchangeHistoryRepository: ExchangeHistoryRepository,
            marketAddress: Address,
            blockDate: Instant,
            blockNumber: Long,
            logIndex: Int,
            punkIndex: BigInteger
        ): OrderCancel? {
            val lastSellEvent = exchangeHistoryRepository
                .findSellEventsByItem(marketAddress, EthUInt256(punkIndex))
                .filter {
                    it.blockNumber!! < blockNumber || it.blockNumber!! == blockNumber && it.logIndex!! < logIndex
                }
                .takeLast(1)
                .singleOrEmpty()
                .awaitFirstOrNull()
                ?.data
            // If the latest exchange event for this punk is OnChainOrder (and not OrderCancel nor OrderSideMatch)
            //  it means that there was a previous sell order. That sell order must be cancelled.
            if (lastSellEvent is OnChainOrder) {
                return OrderCancel(
                    hash = lastSellEvent.hash,
                    maker = lastSellEvent.maker,
                    make = lastSellEvent.make,
                    take = lastSellEvent.take,
                    date = blockDate,
                    source = HistorySource.CRYPTO_PUNKS
                )
            }
            return null
        }
    }
}
