package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.RaribleExchangeV2OrderParser
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ExchangeV2UpsertOrderDescriptor(
    exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val raribleExchangeV2OrderParser: RaribleExchangeV2OrderParser
) : LogEventDescriptor<OnChainOrder> {

    private val exchangeContract = exchangeContractAddresses.v2

    override val collection = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = UpsertOrderEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> {
        return listOf(exchangeContract).toMono()
    }

    override fun convert(log: Log, transaction: Transaction, timestamp: Long): Publisher<OnChainOrder> {
        val event = UpsertOrderEvent.apply(log)
        val order = event.order()
        val date = Instant.ofEpochSecond(timestamp)
        val maker = order._1()
        val taker = order._3().takeUnless { it == Address.ZERO() }
        val make = Asset(order._2()._1().toAssetType(), EthUInt256(order._2()._2()))
        val take = Asset(order._4()._1().toAssetType(), EthUInt256(order._4()._2))
        val salt = EthUInt256(order._5())
        val orderData = raribleExchangeV2OrderParser.convertOrderData(
            version = Binary.apply(order._8()),
            data = Binary.apply(order._9())
        )
        return OnChainOrder(
            maker = maker,
            make = make,
            taker = taker,
            take = take,
            createdAt = date,
            platform = Platform.RARIBLE,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = order._6().toLong().takeUnless { it == 0L },
            end = order._7()?.toLong().takeUnless { it == 0L },
            data = orderData,
            signature = null,
            priceUsd = null,
            hash = Order.hashKey(maker, make.type, take.type, salt.value, orderData)
        ).toMono()
    }
}
