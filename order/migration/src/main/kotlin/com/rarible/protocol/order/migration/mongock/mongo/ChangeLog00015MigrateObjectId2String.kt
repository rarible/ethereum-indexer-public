package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.schema.JsonSchemaObject
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@ChangeLog(order = "000015")
class ChangeLog00015MigrateObjectId2String {

    @ChangeSet(id = "ChangeLog00015MigrateObjectId2String.exchangeHistory", order = "1", author = "protocol")
    fun exchangeHistory(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded operator: TransactionalOperator
    ) = runBlocking {
        logger.info("Starting migration _id fields for ${ExchangeHistoryRepository.COLLECTION}")
        var deleted = 0L
        var inserted = 0L
        val criteria = Criteria.where("_id").type(JsonSchemaObject.Type.OBJECT_ID)
        template.find<LogEvent>(Query(criteria), ExchangeHistoryRepository.COLLECTION).asFlow().collect { logEvent ->
            operator.executeAndAwait { tx ->
                try {
                    val removeQuery = Query.query(Criteria("_id").isEqualTo(ObjectId(logEvent.id)))
                    val dResult = template.remove(removeQuery, ExchangeHistoryRepository.COLLECTION).awaitSingle()
                    deleted += dResult.deletedCount
                    template.insert(logEvent, ExchangeHistoryRepository.COLLECTION).awaitSingle()
                    inserted++
                    if (inserted % 10000L == 0L) {
                        logger.info("Fixed $inserted logs")
                    }
                } catch (ex: Exception) {
                    logger.error("Failed: ", ex)
                    throw ex
                }
            }
        }
        logger.info("Finished migration _id fields for ${ExchangeHistoryRepository.COLLECTION}: $inserted inserted, $deleted deleted")
    }

    @ChangeSet(id = "ChangeLog00015MigrateObjectId2String.orderVersion", order = "2", author = "protocol")
    fun orderVersion(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded operator: TransactionalOperator
    ) = runBlocking {
        logger.info("Starting migration _id fields for ${OrderVersionRepository.COLLECTION}")
        var deleted = 0L
        var inserted = 0L
        val criteria = Criteria.where("_id").type(JsonSchemaObject.Type.OBJECT_ID)
        template.find<OrderVersion>(Query(criteria), OrderVersionRepository.COLLECTION).asFlow().collect { logEvent ->
            operator.executeAndAwait { tx ->
                try {
                    val removeQuery = Query.query(Criteria("_id").isEqualTo(ObjectId(logEvent.id)))
                    val dResult = template.remove(removeQuery, OrderVersionRepository.COLLECTION).awaitSingle()
                    deleted += dResult.deletedCount
                    template.insert(logEvent, OrderVersionRepository.COLLECTION).awaitSingle()
                    inserted++
                    if (inserted % 10000L == 0L) {
                        logger.info("Fixed $inserted logs")
                    }
                } catch (ex: Exception) {
                    logger.error("Failed: ", ex)
                    throw ex
                }
            }
        }
        logger.info("Finished migration _id fields for ${OrderVersionRepository.COLLECTION}: $inserted inserted, $deleted deleted")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00015MigrateObjectId2String::class.java)
    }
}
