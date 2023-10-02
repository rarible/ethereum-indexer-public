package com.rarible.protocol.nft.core.repository

import com.mongodb.client.model.InsertManyOptions
import com.rarible.protocol.nft.core.model.AutoReduce
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
    private val ITEM_COLLECTION = "item_auto_reduce"
    private val TOKEN_COLLECTION = "token_auto_reduce"

    suspend fun saveItems(reduces: Collection<AutoReduce>) {
        insertMany(reduces, ITEM_COLLECTION)
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

    suspend fun saveTokens(reduces: Collection<AutoReduce>) {
        insertMany(reduces, TOKEN_COLLECTION)
    }

    suspend fun removeItem(reduce: AutoReduce) {
        template.remove(reduce, ITEM_COLLECTION).awaitSingle()
    }

    suspend fun removeToken(reduce: AutoReduce) {
        template.remove(reduce, TOKEN_COLLECTION).awaitSingle()
    }

    fun findItems(): Flow<AutoReduce> = template.findAll(AutoReduce::class.java, ITEM_COLLECTION).asFlow()
    fun findTokens(): Flow<AutoReduce> = template.findAll(AutoReduce::class.java, TOKEN_COLLECTION).asFlow()
}
