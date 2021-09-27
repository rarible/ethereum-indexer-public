package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.inValues

@ChangeLog(order = "00009")
class ChangeLog00009AddSourceFieldToOrderHistory {

    @ChangeSet(id = "ChangeLog00009AddSourceFieldToOrderHistory.addSourceFieldToOrderHistory", order = "1", author = "protocol")
    fun removeOrdersWithMakeValueZero(
        @NonLockGuarded historyRepository: ExchangeHistoryRepository,
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val logger = LoggerFactory.getLogger(javaClass)

        logger.info("--- Start add source field to order exchange history")
        var counter = 0L

        val knownClasses = listOf(
            "com.rarible.protocol.order.core.model.OrderSideMatch",
            "com.rarible.protocol.order.core.model.OrderCancel"
        )

        val criteria = Criteria("data._class").inValues(knownClasses)
            .andOperator(LogEvent::data / OrderExchangeHistory::source exists false)
        template.find(Query().addCriteria(criteria), LogEvent::class.java, ExchangeHistoryRepository.COLLECTION)
            .asFlow().collect { log ->
            val updatedLog = when (val data = log.data) {
                is OrderSideMatch -> log.copy(data = data.copy(source = HistorySource.RARIBLE))
                is OrderCancel -> log.copy(data = data.copy(source = HistorySource.RARIBLE))
                else -> throw IllegalStateException("Unexpected data type ${data.javaClass}")
            }
            historyRepository.save(updatedLog).awaitFirst()
            counter++
        }
        logger.info("--- All $counter history was removed")
    }
}
