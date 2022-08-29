package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3
import com.rarible.protocol.order.core.model.OrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.model.OrderX2Y2DataV1
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

interface ItemExchangeHistoryLogEventDescriptor<T : OrderExchangeHistory> : LogEventDescriptor<T> {
    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<T> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<T>
}

fun getOriginMaker(maker: Address, data: OrderData?): Address {
    return when (data) {
        is OrderRaribleV2DataV1 -> if (data.payouts.isSingleton) data.payouts.first().account else maker
        is OrderRaribleV2DataV2 -> if (data.payouts.isSingleton) data.payouts.first().account else maker
        is OrderRaribleV2DataV3 -> data.payout?.account ?: maker
        is OrderDataLegacy, is OrderOpenSeaV1DataV1, is OrderBasicSeaportDataV1, is OrderCryptoPunksData,
        is OrderX2Y2DataV1,
        is OrderLooksrareDataV1,
        is OrderSudoSwapAmmDataV1 -> maker
        null -> maker
    }
}
