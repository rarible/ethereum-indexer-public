package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@ChangeLog(order = "000019")
class ChangeLog00019AddDbUpdatedToOrder {
    @ChangeSet(id = "ChangeLog00019AddDbUpdatedToOrder.addDbUpdatedFieldToOrder", order = "1", author = "protocol")
    fun addDbUpdatedFieldToOrder(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking<Unit> {
        val queryMulti = Query(Criteria.where(Order::dbUpdatedAt.name).exists(false))
        val multiUpdate = AggregationUpdate.update()
            .set(Order::dbUpdatedAt.name).toValue("\$${Order::createdAt.name}")
        template.updateMulti(queryMulti, multiUpdate, MongoOrderRepository.COLLECTION).awaitFirst()
    }
}