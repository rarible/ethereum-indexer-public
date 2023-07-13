package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ReduceCanceledSeaportTaskHandler(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderUpdateService: OrderUpdateService
) : TaskHandler<CancelOpenSeaState> {

    val logger: Logger = LoggerFactory.getLogger(ReduceCanceledSeaportTaskHandler::class.java)

    override val type: String
        get() = REDUCE_CANCELED_SEAPORT_ORDERS

    override fun runLongTask(from: CancelOpenSeaState?, param: String): Flow<CancelOpenSeaState> {

        // Default value when new seaport was released
        val startParam = from ?: CancelOpenSeaState(Instant.parse("2023-03-01T00:00:00Z"), ObjectId.get().toString())

        logger.info("Start $REDUCE_CANCELED_SEAPORT_ORDERS task with param from $startParam")
        val date = LogEvent::data / OrderExchangeHistory::date
        val criteria = (LogEvent::data / OrderExchangeHistory::make / Asset::type / NftAssetType::nft).isEqualTo(true)
            .and(LogEvent::data / OrderExchangeHistory::type).isEqualTo(ItemType.CANCEL)
            .orOperator(
                date gt startParam.date,
                (date isEqualTo startParam.date).and(LogEvent::id).gt(ObjectId(startParam.id))
            )
            .and(LogEvent::data / OrderExchangeHistory::source).isEqualTo(HistorySource.OPEN_SEA)
        return exchangeHistoryRepository.find(
            Query(criteria)
                .with(
                    Sort.by(
                        Sort.Order.asc("${LogEvent::data.name}.${OrderExchangeHistory::date.name}"),
                        Sort.Order.asc(OrderVersion::id.name)
                    )
                )
        ).map { log ->
            val cancel = log.data as OrderExchangeHistory
            orderUpdateService.update(cancel.hash, orderTaskEventMarks())
            CancelOpenSeaState(cancel.date, log.id.toString())
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ReduceCanceledSeaportTaskHandler::class.java)
        const val REDUCE_CANCELED_SEAPORT_ORDERS = "REDUCE_CANCELED_SEAPORT_ORDERS"
    }
}

data class CancelOpenSeaState(
    val date: Instant,
    val id: String
)
