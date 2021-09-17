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
    private val transferProxyAddresses: OrderIndexerProperties.TransferProxyAddresses,
    private val traceProvider: TransactionTraceProvider
) : ItemExchangeHistoryLogEventDescriptor<OnChainOrder> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkOfferedEvent.id()

    override suspend fun convert(log: Log, date: Instant): List<OnChainOrder> {
        val punkOfferedEvent = PunkOfferedEvent.apply(log)
        val grantedBuyer = punkOfferedEvent.toAddress().takeUnless { it == Address.ZERO() }
        if (grantedBuyer == transferProxyAddresses.cryptoPunksTransferProxy) {
            // We ignore "grant buy for 0ETH" from the owner to the transfer proxy,
            // because this is the Exchange contract's implementation detail.
            return emptyList()
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
        val sellerAddress = transactionTrace.from
        val make = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)), EthUInt256.ONE)
        val take = Asset(EthAssetType, EthUInt256(minPrice))
        val sellOrderHash = Order.hashKey(sellerAddress, make.type, take.type, CRYPTO_PUNKS_SALT.value)
        return listOf(
            OnChainOrder(
                OrderVersion(
                    hash = sellOrderHash,
                    maker = sellerAddress,
                    taker = grantedBuyer,
                    make = make,
                    take = take,
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
                )
            )
        )
    }
}
