package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.mongodb.client.result.UpdateResult
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@ChangeLog(order = "00019")
class ChangeLog000019OrderVersionUpdate {

    @ChangeSet(
        id = "ChangeLog00019OrderVersionUpdate",
        order = "1",
        author = "protocol"
    )
    fun updateOrderVersion(orderVersionRepository: OrderVersionRepository): UpdateResult = runBlocking {
        val queryMulti = Query(Criteria.where(LogEvent::updatedAt.name).exists(false))
        val multiUpdate = AggregationUpdate.update()
            .set(LogEvent::updatedAt.name).toValue("\$${LogEvent::createdAt.name}")
        orderVersionRepository.updateMulti(queryMulti, multiUpdate).awaitFirst()
    }
}
