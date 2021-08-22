package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.CryptoPunksMarket
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkNoLongerForSaleEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.trace.TransactionTraceProvider
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class CryptoPunkNoLongerForSaleLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val traceProvider: TransactionTraceProvider
) : ItemExchangeHistoryLogEventDescriptor<OrderCancel> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkNoLongerForSaleEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderCancel> {
        val noLongerForSaleEvent = PunkNoLongerForSaleEvent.apply(log)
        val punkIndex = EthUInt256(noLongerForSaleEvent.punkIndex())
        val transactionTrace = traceProvider.getTransactionTrace(log.transactionHash())
        if (transactionTrace == null) {
            logger.info(
                "Transaction trace is not available for ${log.transactionHash()} " +
                        "for sell cancellation of punk #${punkIndex.value}"
            )
            return emptyList()
        }
        if (!transactionTrace.input.startsWith(CryptoPunksMarket.punkNoLongerForSaleSignature().id().prefixed())) {
            /*
            We must ignore 'PunkNoLongerForSale' event if it was emitted during 'buyPunk' or 'transfer' function,
            because the order is actually filled, not cancelled.
             */
            return emptyList()
        }
        val ownerAddress = transactionTrace.from
        val marketAddress = log.address()
        val orderHash = Order.hashKey(
            maker = ownerAddress,
            makeAssetType = CryptoPunksAssetType(marketAddress, punkIndex.value.toInt()),
            takeAssetType = EthAssetType,
            salt = CRYPTO_PUNKS_SALT.value
        )
        return listOf(
            OrderCancel(
                hash = orderHash,
                maker = ownerAddress,
                make = Asset(CryptoPunksAssetType(marketAddress, punkIndex.value.toInt()), EthUInt256.ONE),
                take = Asset(EthAssetType, EthUInt256.ZERO),
                date = date,
                source = HistorySource.CRYPTO_PUNKS
            )
        )
    }
}