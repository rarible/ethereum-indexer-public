package com.rarible.protocol.order.listener.service.task

import com.rarible.core.task.TaskHandler
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
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
                val result = OrderActivityResult.History(history)
                val dto = orderActivityConverter.convert(result, reverted = true)
                    ?: error("Can't convert order history ${history.id}")

                publisher.publish(dto)
                saveWithRevertedStatus(history)
                logger.info("Published ad saved ignored activity ${history.id}")
                history.id.toHexString()
            }
    }

    private suspend fun saveWithRevertedStatus(logEvent: LogEvent) {
        exchangeHistoryRepository.save(logEvent.copy(status = LogEventStatus.REVERTED)).awaitFirst()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RevertIgnoredActivitiesTaskHandler::class.java)
        const val REVERT_IGNORED_ACTIVITIES = "REVERT_IGNORED_ACTIVITIES"
    }
}
