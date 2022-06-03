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
import java.time.Instant

@ChangeLog(order = "00022")
class ChangeLog00022dbUpdateAt {
    @ChangeSet(
        id = "ChangeLog00022dbUpdateAt.updateToken",
        order = "1",
        author = "protocol"
    )
    fun updateToken(@NonLockGuarded template: ReactiveMongoTemplate) = runBlocking<Unit> {
        val queryMulti = Query(Criteria.where(Token::dbUpdatedAt.name).exists(false))
        val multiUpdate = AggregationUpdate.update()
            .set(Token::dbUpdatedAt.name).toValue(Instant.EPOCH)
        template.updateMulti(queryMulti, multiUpdate, "token").awaitFirst()
    }
}