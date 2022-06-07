package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import com.rarible.protocol.nft.core.model.Token
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

@ChangeLog(order = "00022")
class ChangeLog00022dbUpdateAt {
    @ChangeSet(
        id = "ChangeLog00022dbUpdateAt.updateToken",
        order = "1",
        author = "protocol"
    )
    fun updateToken(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking<Unit> {
        val collectionName = "token"
        val infoAmount = 1000

        val selectQuery = Query(Criteria.where(Token::dbUpdatedAt.name).exists(false))
        val updateAmount = template.find(selectQuery, Token::class.java, collectionName).asFlow().count()

        repeat(updateAmount) {
            val update = AggregationUpdate.update().set(Token::dbUpdatedAt.name).toValue(Instant.now())
            template.updateFirst(selectQuery, update, collectionName).awaitFirst()
            if ((it+1) % infoAmount == 0) {
                logger.info("${it+1} tokens has been updated!")
            }
        }
        logger.info("All tokens has been updated!")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00022dbUpdateAt::class.java)
    }
}