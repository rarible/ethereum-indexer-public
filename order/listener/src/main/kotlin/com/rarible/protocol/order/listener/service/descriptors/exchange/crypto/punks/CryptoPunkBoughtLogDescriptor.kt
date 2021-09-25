package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.TransferEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.request.LogFilter
import scalether.domain.request.TopicFilter
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant

@Service
class CryptoPunkBoughtLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
    private val ethereum: MonoEthereum,
    private val traceProvider: TransactionTraceProvider
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    private val logger = LoggerFactory.getLogger(CryptoPunkBoughtLogDescriptor::class.java)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBoughtEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderExchangeHistory> {
        val punkBoughtEvent = PunkBoughtEvent.apply(log)
        val marketAddress = log.address()
        val cryptoPunksAssetType = CryptoPunksAssetType(marketAddress, EthUInt256(punkBoughtEvent.punkIndex()))
        val sellerAddress = punkBoughtEvent.fromAddress()
        val buyerAddress = getBuyerAddress(punkBoughtEvent)
        if (buyerAddress == transferProxyAddresses.cryptoPunksTransferProxy) {
            // We ignore "buy for 0ETH" from the owner to the transfer proxy events,
            // because this is the Exchange contract's implementation detail.
            return emptyList()
        }
        val calledFunctionSignature = getCalledFunctionSignature(punkBoughtEvent)
        val transactionTrace = traceProvider.getTransactionTrace(punkBoughtEvent.log().transactionHash())
        val punkPrice = getPunkPrice(punkBoughtEvent, calledFunctionSignature, transactionTrace)
        val externalOrderExecutedOnRarible = isExternalOrderExecutedOnRarible(transactionTrace)
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
        return listOf(
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
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible
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
                externalOrderExecutedOnRarible = externalOrderExecutedOnRarible
            )
        )
    }

    private fun isExternalOrderExecutedOnRarible(transactionTrace: SimpleTraceResult?): Boolean {
        transactionTrace ?: return false
        return transactionTrace.input.endsWith(Platform.CRYPTO_PUNKS.id.hex())
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
        transactionTrace: SimpleTraceResult?
    ): BigInteger {
        if (punkBoughtEvent.value() != BigInteger.ZERO || calledFunctionSignature != CryptoPunksMarket.acceptBidForPunkSignature().name()) {
            return punkBoughtEvent.value()
        }
        // Because of https://github.com/larvalabs/cryptopunks/issues/19 we cannot extract the correct "bid.value" for "acceptBidForPunk" function.
        // Thus, we try to guess the value from the "minPrice" parameter passed to transaction.
        // We consider that "minPrice" == "bid.value". Of course this may not be true:
        // 1) Seller might have set "minPrice = 0" when he saw the punk bid, which he was ready to accept.
        // 2) There might have been another bid with bigger "bid.value" appeared before the "acceptBidForPunk" transaction was accepted.
        if (transactionTrace == null) {
            logger.warn("Unable to get transaction trace for ${punkBoughtEvent.log().transactionHash()}")
            return BigInteger.ZERO
        }
        val decodedInput = CryptoPunksMarket.acceptBidForPunkSignature().`in`().decode(
            Binary.apply(transactionTrace.input),
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
            return Address.ZERO()
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
}
