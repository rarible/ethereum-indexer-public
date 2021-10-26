package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkNoLongerForSaleEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.service.descriptors.ItemExchangeHistoryLogEventDescriptor
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
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
class CryptoPunkNoLongerForSaleLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val ethereum: MonoEthereum
) : ItemExchangeHistoryLogEventDescriptor<OrderExchangeHistory> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkNoLongerForSaleEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderExchangeHistory> {
        if (shouldIgnoreThisLog(log)) {
            return emptyList()
        }
        val noLongerForSaleEvent = PunkNoLongerForSaleEvent.apply(log)
        val punkIndex = EthUInt256(noLongerForSaleEvent.punkIndex())
        val ownerAddress = transaction.from()
        val marketAddress = log.address()
        val orderHash = Order.hashKey(
            maker = ownerAddress,
            makeAssetType = CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)),
            takeAssetType = EthAssetType,
            salt = CRYPTO_PUNKS_SALT.value
        )
        return listOf(
            OrderCancel(
                hash = orderHash,
                maker = ownerAddress,
                make = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)), EthUInt256.ONE),
                take = Asset(EthAssetType, EthUInt256.ZERO),
                date = date,
                source = HistorySource.CRYPTO_PUNKS
            )
        )
    }

    /**
     * We must ignore 'PunkNoLongerForSale' event if it was emitted during 'buyPunk' function,
     * because the order is actually filled, not cancelled. To determine this, we try to find the subsequent 'PunkBought' event.
     */
    private suspend fun shouldIgnoreThisLog(log: Log): Boolean {
        val filter = LogFilter
            .apply(TopicFilter.or(PunkBoughtEvent.id()))
            .address(exchangeContractAddresses.cryptoPunks)
            .blockHash(log.blockHash())
        val blockLogs = try {
            ethereum.ethGetLogsJava(filter).awaitSingle()
        } catch (e: Exception) {
            logger.warn("Unable to get logs for block ${log.blockHash()}", e)
            return true
        }
        return blockLogs.any { blockLog ->
            blockLog.transactionHash() == log.transactionHash()
                    && blockLog.logIndex() == log.logIndex().plus(BigInteger.ONE)
        }
    }
}
