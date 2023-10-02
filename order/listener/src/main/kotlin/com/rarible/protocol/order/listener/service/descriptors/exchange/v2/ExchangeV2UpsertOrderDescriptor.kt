package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.exchange.v2.events.UpsertOrderEvent
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toAssetType
import com.rarible.protocol.order.core.parser.ExchangeV2OrderDataParser
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Service
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
class ExchangeV2UpsertOrderDescriptor(
    contractsProvider: ContractsProvider,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OnChainOrder>(
    name = "rari_v2_upsert",
    topic = UpsertOrderEvent.id(),
    contracts = contractsProvider.raribleExchangeV2(),
    autoReduceService = autoReduceService,
) {
    override suspend fun convert(log: Log, transaction: Transaction, timestamp: Instant, index: Int, totalLogs: Int): List<OnChainOrder> {
        val event = UpsertOrderEvent.apply(log)
        val order = event.order()
        val maker = order._1()
        val taker = order._3().takeUnless { it == Address.ZERO() }
        val make = Asset(order._2()._1().toAssetType(), EthUInt256(order._2()._2()))
        val take = Asset(order._4()._1().toAssetType(), EthUInt256(order._4()._2))
        val salt = EthUInt256(order._5())
        val orderData = ExchangeV2OrderDataParser.parse(
            version = Binary.apply(order._8()),
            data = Binary.apply(order._9())
        )
        return listOf(
            OnChainOrder(
                maker = maker,
                make = make,
                taker = taker,
                take = take,
                createdAt = timestamp,
                platform = Platform.RARIBLE,
                orderType = OrderType.RARIBLE_V2,
                salt = salt,
                start = order._6().toLong().takeUnless { it == 0L },
                end = order._7()?.toLong().takeUnless { it == 0L },
                data = orderData,
                signature = null,
                priceUsd = null,
                hash = Order.hashKey(maker, make.type, take.type, salt.value, orderData)
            )
        )
    }
}
