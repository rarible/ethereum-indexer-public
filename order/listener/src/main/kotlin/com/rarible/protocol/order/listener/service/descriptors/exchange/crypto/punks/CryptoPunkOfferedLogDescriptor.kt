package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkOfferedEvent
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
class CryptoPunkOfferedLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val traceProvider: TransactionTraceProvider
) : ItemExchangeHistoryLogEventDescriptor<OrderNew> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkOfferedEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OrderNew> {
        val punkOfferedEvent = PunkOfferedEvent.apply(log)
        if (punkOfferedEvent.toAddress() != Address.ZERO()) {
            logger.info(
                "Ignore offering of punk #${punkOfferedEvent.punkIndex()} to a concrete address " +
                        "(${punkOfferedEvent.toAddress()}), listen to only public offerings."
            )
            return emptyList()
        }
        val minPrice = punkOfferedEvent.minValue()
        val punkIndex = EthUInt256(punkOfferedEvent.punkIndex())
        val transactionTrace = traceProvider.getTransactionTrace(log.transactionHash())
        if (transactionTrace == null) {
            logger.info(
                "Transaction trace is not available for ${log.transactionHash()} " +
                        "for offering of punk #${punkOfferedEvent.punkIndex()}"
            )
            return emptyList()
        }
        val marketAddress = log.address()
        return listOf(
            OrderNew(
                Order(
                    maker = transactionTrace.from,
                    taker = null,
                    make = Asset(CryptoPunksAssetType(marketAddress, punkIndex.value.toInt()), EthUInt256.ONE),
                    take = Asset(EthAssetType, EthUInt256(minPrice)),
                    type = OrderType.CRYPTO_PUNKS,
                    fill = EthUInt256.ZERO,
                    cancelled = false,

                    //TODO[punk]: not sure about these values:
                    makeStock = EthUInt256.ZERO,
                    salt = EthUInt256.ZERO,
                    start = null,
                    end = null,
                    data = OrderCryptoPunksData(marketAddress),
                    signature = null,
                    createdAt = date,
                    lastUpdateAt = date,
                    platform = Platform.CRYPTO_PUNKS
                )
            )
        )
    }
}