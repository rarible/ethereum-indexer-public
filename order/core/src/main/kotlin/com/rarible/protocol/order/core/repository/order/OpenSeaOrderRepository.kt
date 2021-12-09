package com.rarible.protocol.order.core.repository.order

import com.mongodb.reactivestreams.client.MongoClient
import com.rarible.protocol.order.core.model.RawOpenSeaOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
 
@Component
class OpenSeaOrderRepository(mongoClient: MongoClient) {
    private val template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(mongoClient, "open-sea"))

    suspend fun save(order: RawOpenSeaOrder): RawOpenSeaOrder {
        return template.save(order).awaitFirst()
    }

    fun getAll(from: String?): Flow<RawOpenSeaOrder> {
        val criteria = from?.let { Criteria.where("_id").gt(from) } ?: Criteria()
        val sort = Sort.by(Sort.Order.asc("_id"))
        return template.find<RawOpenSeaOrder>(Query.query(criteria).with(sort)).asFlow()
    }
}
