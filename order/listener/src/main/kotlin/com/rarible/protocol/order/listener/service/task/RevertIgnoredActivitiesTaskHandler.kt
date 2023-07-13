package com.rarible.protocol.order.listener.service.task

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.misc.orderTaskEventMarks
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RevertIgnoredActivitiesTaskHandler(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val publisher: ProtocolOrderPublisher,
    private val orderActivityConverter: OrderActivityConverter,
) : TaskHandler<String> {

    override val type: String
        get() = REVERT_IGNORED_ACTIVITIES

    override suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return exchangeHistoryRepository.findIgnoredEvents(from, HistorySource.valueOf(param))
            .map { history ->
                val eventTimeMarks = orderTaskEventMarks()
                val result = OrderActivityResult.History(history)
                val dto = orderActivityConverter.convert(result, reverted = true)
                    ?: error("Can't convert order history ${history.id}")

                publisher.publish(dto, eventTimeMarks)
                saveWithRevertedStatus(history)
                logger.info("Published ad saved ignored activity ${history.id}")
                history.id
            }
    }

    private suspend fun saveWithRevertedStatus(logEvent: ReversedEthereumLogRecord) {
        exchangeHistoryRepository.save(logEvent.copy(status = EthereumBlockStatus.REVERTED)).awaitFirst()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RevertIgnoredActivitiesTaskHandler::class.java)
        const val REVERT_IGNORED_ACTIVITIES = "REVERT_IGNORED_ACTIVITIES"
    }
}
