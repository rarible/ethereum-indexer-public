package com.rarible.protocol.order.core.repository

import com.mongodb.client.model.InsertManyOptions
import com.rarible.protocol.order.core.model.AutoReduce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.stereotype.Component

@Component
class AutoReduceRepository(
    private val template: ReactiveMongoOperations,
    private val mongoConverter: MongoConverter,
) {
    private val ORDER_COLLECTION = "order_auto_reduce"
    private val AUCTION_COLLECTION = "auction_auto_reduce"

    suspend fun saveOrders(reduces: Collection<AutoReduce>) {
        insertMany(reduces, ORDER_COLLECTION)
    }

    suspend fun saveAuctions(reduces: Collection<AutoReduce>) {
        insertMany(reduces, AUCTION_COLLECTION)
    }

    private suspend fun insertMany(reduces: Collection<AutoReduce>, collection: String) {
        template.execute { database ->
            val documents = reduces.map { reduce ->
                val document = Document()
                mongoConverter.write(reduce, document)
                document
            }
            database.getCollection(collection).insertMany(documents, InsertManyOptions().ordered(false))
        }.onErrorComplete().awaitFirstOrNull()
    }

    suspend fun removeOrder(reduce: AutoReduce) {
        template.remove(reduce, ORDER_COLLECTION).awaitSingle()
    }

    suspend fun removeAuction(reduce: AutoReduce) {
        template.remove(reduce, AUCTION_COLLECTION).awaitSingle()
    }

    fun findOrders(): Flow<AutoReduce> = template.findAll(AutoReduce::class.java, ORDER_COLLECTION).asFlow()
    fun findAuctions(): Flow<AutoReduce> = template.findAll(AutoReduce::class.java, AUCTION_COLLECTION).asFlow()
}
