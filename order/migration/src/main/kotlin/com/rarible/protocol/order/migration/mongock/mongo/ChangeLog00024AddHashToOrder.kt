package com.rarible.protocol.order.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate
import org.springframework.data.mongodb.core.query.Query

@ChangeLog(order = "000024")
class ChangeLog00024AddHashToOrder {
    @ChangeSet(id = "ChangeLog00024AddHashToOrder.addHashFieldToOrder", order = "1", author = "protocol")
    fun addHashFieldToOrder(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking<Unit> {
        template.updateMulti(
            Query(),
            AggregationUpdate.update().set(Order::hash.name).toValue("\$_id"),
            MongoOrderRepository.COLLECTION
        ).awaitSingle()
    }
}
