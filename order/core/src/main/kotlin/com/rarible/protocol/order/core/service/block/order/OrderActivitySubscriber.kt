package com.rarible.protocol.order.core.service.block.order

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("order-event-subscriber")
class OrderActivitySubscriber(
    private val eventPublisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter
) : LogRecordEventSubscriber {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val indexerInMark = nowMillis()
        events.forEach { event ->
            val logRecord = event.record.asEthereumLogRecord()
            val eventTimeMarks = event.eventTimeMarks.addIndexerIn(indexerInMark)

            val dataType = logRecord.data::class.java.simpleName
            logger.info("Order log event: id=${logRecord.id}, dataType=$dataType, reverted=${event.reverted}")

            convert(logRecord, event.reverted)?.let {
                eventPublisher.publish(it, event.eventTimeMarks.addIndexerIn(indexerInMark))
            }
        }
    }

    private suspend fun convert(logRecord: ReversedEthereumLogRecord, reverted: Boolean): OrderActivityDto? {
        return if (
            (logRecord.data as? OrderSideMatch)?.side == OrderSide.LEFT ||
            logRecord.data is OnChainOrder ||
            logRecord.data is OrderCancel
        ) {
            orderActivityConverter.convert(
                OrderActivityResult.History(logRecord),
                reverted
            )
        } else {
            null
        }
    }
}
