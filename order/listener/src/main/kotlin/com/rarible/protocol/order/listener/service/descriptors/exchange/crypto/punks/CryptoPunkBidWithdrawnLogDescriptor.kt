package com.rarible.protocol.order.listener.service.descriptors.exchange.crypto.punks

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.crypto.punks.PunkBidWithdrawnEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.*
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
class CryptoPunkBidWithdrawnLogDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses
) : ItemExchangeHistoryLogEventDescriptor<OrderCancel> {

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.cryptoPunks))

    override val topic: Word = PunkBidWithdrawnEvent.id()

    override suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderCancel> {
        val bidWithdrawnEvent = PunkBidWithdrawnEvent.apply(log)
        val punkIndex = EthUInt256(bidWithdrawnEvent.punkIndex())
        val bidderAddress = bidWithdrawnEvent.fromAddress()
        val marketAddress = log.address()
        val orderHash = Order.hashKey(
            maker = bidderAddress,
            makeAssetType = EthAssetType,
            takeAssetType = CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)),
            salt = CRYPTO_PUNKS_SALT.value
        )
        return listOf(
            OrderCancel(
                hash = orderHash,
                maker = bidderAddress,
                make = Asset(EthAssetType, EthUInt256.ZERO),
                take = Asset(CryptoPunksAssetType(marketAddress, EthUInt256(punkIndex.value)), EthUInt256.ONE),
                date = date,
                source = HistorySource.CRYPTO_PUNKS
            )
        )
    }
}
