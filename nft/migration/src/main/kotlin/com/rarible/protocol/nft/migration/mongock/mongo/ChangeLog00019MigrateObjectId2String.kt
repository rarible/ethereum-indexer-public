package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
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

@ChangeLog(order = "00019")
class ChangeLog00019MigrateObjectId2String {
    @ChangeSet(
        id = "ChangeLog00019MigrateObjectId2String.setStringId",
        order = "1",
        author = "protocol"
    )
    fun setStringId(
        @NonLockGuarded template: ReactiveMongoTemplate,
        @NonLockGuarded operator: TransactionalOperator
    ) = runBlocking {
        logger.info("Started setting string as _id field")
        var deleted = 0L
        var inserted = 0L
        val criteria = Criteria.where("_id").type(JsonSchemaObject.Type.OBJECT_ID)
        template.find<LogEvent>(Query(criteria), NftItemHistoryRepository.COLLECTION).asFlow().collect { logEvent ->
            operator.executeAndAwait { tx ->
                try {
                    val removeQuery = Query.query(Criteria("_id").isEqualTo(ObjectId(logEvent.id)))
                    val dResult = template.remove(removeQuery, NftItemHistoryRepository.COLLECTION).awaitSingle()
                    deleted += dResult.deletedCount
                    template.insert(logEvent, NftItemHistoryRepository.COLLECTION).awaitSingle()
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
        logger.info("Finished setting string as _id field: $inserted inserted, $deleted deleted")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00019MigrateObjectId2String::class.java)
    }
}
