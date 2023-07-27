package com.rarible.protocol.order.core.repository.order

import com.mongodb.client.result.UpdateResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.api.misc.indexName
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.LogEventKey
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CaptureSpan(type = SpanType.DB)
@Component
class OrderVersionRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun dropIndexes() {
        dropIndexes(
            "make.type.nft_1_createdAt_-1_id_-1",
            "make.type.nft_1_make.type.token_1_createdAt_-1_id_-1",
            "make.type.nft_1_maker_-1_createdAt_-1_id_-1",
            "make.type.token_1_make.type.tokenId_1_createdAt_-1_id_-1",
            "take.type.nft_1_createdAt_-1_id_-1",
            "take.type.nft_1_maker_-1_createdAt_-1_id_-1",
            "take.type.nft_1_take.type.token_1_createdAt_-1_id_-1",
            "take.type.token_1_take.type.tokenId_1_createdAt_-1_id_-1",
            "take.type.token_1_take.type.tokenId_1_createdAt_1_takePriceUsd_1__id_1",
            "hash_1",
            "makeStock_-1_lastUpdateAt_-1__id_1",
            OrderVersionRepositoryIndexes.MAKER_TAKE_PRICE_USD_BID_DEFINITION.indexName,
            OrderVersionRepositoryIndexes.HASH_PLATFORM_AND_ID_DEFINITION.indexName
        )
    }

    private suspend fun dropIndexes(vararg names: String) {
        val existing = template.indexOps(COLLECTION).indexInfo.map { it.name }.collectList().awaitFirst()
        for (name in names) {
            if (existing.contains(name)) {
                logger.info("dropping index $name")
                template.indexOps(COLLECTION).dropIndex(name).awaitFirstOrNull()
            } else {
                logger.info("skipping drop index $name")
            }
        }
    }

    suspend fun createIndexes() {
        OrderVersionRepositoryIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    fun save(orderVersion: OrderVersion): Mono<OrderVersion> {
        return template.save(orderVersion, COLLECTION)
    }

    fun updateMulti(query: Query, update: UpdateDefinition): Mono<UpdateResult> {
        return template.updateMulti(query, update, COLLECTION)
    }

    fun find(query: Query): Flow<OrderVersion> {
        return template.query<OrderVersion>().matching(query).all().asFlow()
    }

    fun search(filter: OrderVersionFilter): Flux<OrderVersion> {
        val limit = filter.limit
        val hint = filter.hint
        val sort = filter.sort
        val criteria = filter.getCriteria()

        val query = Query(criteria)

        if (hint != null) {
            query.withHint(hint)
        }
        query.with(sort)
        if (limit != null) {
            query.limit(limit)
        }
        return template.find(query, COLLECTION)
    }

    fun deleteAll(): Flux<OrderVersion> {
        return template.findAllAndRemove<OrderVersion>(Query(), COLLECTION)
    }

    fun count(): Mono<Long> {
        return template.count(Query(), COLLECTION)
    }

    fun findAll(): Flux<OrderVersion> {
        return template.findAll(COLLECTION)
    }

    fun findAllByHash(hash: Word): Flow<OrderVersion> = findAllByHash(hash, null, null).asFlow()

    suspend fun findLatestByHash(hash: Word): OrderVersion? =
        template.find<OrderVersion>(
            Query(where(OrderVersion::hash).isEqualTo(hash)).with(
                Sort.by(
                    Sort.Direction.DESC,
                    OrderVersion::id.name
                )
            ), COLLECTION
        ).asFlow().firstOrNull()

    fun findAllByHash(hash: Word?, fromHash: Word?, platforms: List<Platform>?): Flux<OrderVersion> {
        var criteria = when {
            hash != null -> OrderVersion::hash isEqualTo hash
            fromHash != null -> OrderVersion::hash gt fromHash
            else -> Criteria()
        }
        if (platforms?.isNotEmpty() == true) {
            criteria = criteria.and(OrderVersion::platform).inValues(platforms)
        }
        val query = Query(criteria).with(HASH_SORT_ASC)
        return template.find(query, COLLECTION)
    }

    fun findById(id: ObjectId): Mono<OrderVersion> {
        return template.findById(id)
    }

    fun existsByOnChainOrderKey(key: LogEventKey): Mono<Boolean> {
        val criteria = OrderVersion::onChainOrderKey / LogEventKey::databaseKey isEqualTo key.databaseKey
        val query = Query(criteria)
        return template.exists(query, COLLECTION)
    }

    fun deleteByOnChainOrderKey(key: LogEventKey): Mono<Void> {
        val criteria = OrderVersion::onChainOrderKey isEqualTo key
        return template.remove(Query(criteria), COLLECTION).then()
    }

    suspend fun deleteByHash(hash: Word) {
        val criteria = OrderVersion::hash isEqualTo hash
        template.remove(Query(criteria), COLLECTION).then().awaitFirstOrNull()
    }

    fun findByIds(ids: List<ObjectId>): Flux<OrderVersion> {
        val query = Query(OrderVersion::id inValues ids)
        return template.find(query, OrderVersion::class.java, COLLECTION)
    }

    companion object {
        const val COLLECTION = "order_version"
        val logger: Logger = LoggerFactory.getLogger(OrderVersionRepository::class.java)

        val HASH_SORT_ASC: Sort = Sort.by(OrderVersion::hash.name, "_id")
    }
}
