package com.rarible.protocol.order.core.repository.order

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.lte
import org.springframework.data.mongodb.core.query.ne
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.util.*

@CaptureSpan(type = SpanType.DB)
@Component
class MongoOrderRepository(
    private val template: ReactiveMongoOperations
) : OrderRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun dropIndexes() {
        dropIndexes(
            "make.type.token_1_make.type.tokenId_1_lastUpdateAt_1__id_1",
            "end_1_start_1_makeStock_1__id_1",
            "make.type.nft_1_createdAt_1__id_1",  // Incorrect SELL_ORDERS_DEFINITION
            "make.type.nft_1_platform_1_createdAt_1__id_1",// Incorrect SELL_ORDERS_PLATFORM_DEFINITION
            "makeStock_-1_lastUpdateAt_-1",
            "makeStock_-1_lastUpdateAt_-1__id_1",
            "makeStock_1_lastUpdateAt_1__id_1"
        )
    }

    override suspend fun createIndexes() {
        OrderRepositoryIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    override suspend fun save(order: Order): Order {
        return template.save(order).awaitFirst()
    }

    override suspend fun findById(hash: Word): Order? {
        return template.findById<Order>(hash).awaitFirstOrNull()
    }

    override fun findAll(hashes: Collection<Word>): Flow<Order> {
        val criteria = Criteria.where("_id").inValues(hashes)
        return template.find<Order>(Query.query(criteria)).asFlow()
    }

    override suspend fun search(query: Query): List<Order> {
        return template.query<Order>()
            .matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    override suspend fun remove(hash: Word): Boolean {
        val criteria = Criteria.where("_id").isEqualTo(hash)
        return template.remove(Query(criteria), Order::class.java).awaitFirst().deletedCount > 0
    }

    override fun findActive(): Flow<Order> {
        return template.query<Order>().matching(
            Query(
                Criteria().andOperator(
                    Order::cancelled isEqualTo false,
                    Order::makeStock ne EthUInt256.ZERO
                )
            )
        ).all().asFlow()
    }

    override fun findAll(): Flow<Order> {
        return template.findAll<Order>().asFlow()
    }

    override fun findAll(platform: Platform, status: OrderStatus, fromHash: Word?): Flow<Order> {
        return template.query<Order>().matching(
            Query(
                Criteria().andOperator(
                    listOfNotNull(
                        Order::platform isEqualTo platform,
                        Order::status isEqualTo status,
                        if (fromHash != null) Order::hash gt fromHash else null
                    )
                )
            ).withHint(OrderRepositoryIndexes.BY_LAST_UPDATE_AND_STATUS_AND_PLATFORM_AND_ID_DEFINITION.indexKeys)
                .with(Sort.by(Order::platform.name, Order::status.name, Order::lastUpdateAt.name, "_id"))
        ).all().asFlow()
    }

    override fun findByTargetNftAndNotCanceled(maker: Address, token: Address, tokenId: EthUInt256): Flow<Order> {
        val criteria =
            (Order::make / Asset::type / NftAssetType::token isEqualTo token)
                .and(Order::make / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)
                .and(Order::maker).isEqualTo(maker)
                .and(Order::cancelled).isEqualTo(false)

        val query = Query(criteria)
        return template.query<Order>().matching(query).all().asFlow()
    }

    override fun findTakeTypesOfSellOrders(token: Address, tokenId: EthUInt256): Flow<AssetType> {
        val criteria = Criteria().andOperator(
            Order::make / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::make / Asset::type / NftAssetType::tokenId isEqualTo tokenId,
                (Order::make / Asset::type / NftAssetType::tokenId exists false)
                    .and(Order::make / Asset::type / NftAssetType::nft).isEqualTo(true)
            )
        )
        return template.findDistinct<AssetType>(
            Query(criteria),
            "${Order::take.name}.${Asset::type.name}",
            Order::class.java,
            AssetType::class.java
        ).asFlow()
    }

    override fun findMakeTypesOfBidOrders(token: Address, tokenId: EthUInt256): Flow<AssetType> {
        @Suppress("DuplicatedCode")
        val criteria = Criteria().andOperator(
            Order::take / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::take / Asset::type / NftAssetType::tokenId isEqualTo tokenId,
                (Order::take / Asset::type / NftAssetType::tokenId exists false)
                    .and(Order::take / Asset::type / NftAssetType::nft).isEqualTo(true)
            )
        )
        return template.findDistinct<AssetType>(
            Query(criteria),
            "${Order::make.name}.${Asset::type.name}",
            Order::class.java,
            AssetType::class.java
        ).asFlow()
    }

    override suspend fun findByMake(token: Address, tokenId: EthUInt256): Order? {
        val criteria = (Order::status isEqualTo OrderStatus.ACTIVE)
            .and(Order::make / Asset::type / NftAssetType::token).isEqualTo(token)
            .and(Order::make / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)
        return template.find(Query(criteria), Order::class.java).awaitFirstOrNull()
    }

    override suspend fun findOpenSeaHashesByMakerAndByNonce(
        maker: Address,
        fromIncluding: Long,
        toExcluding: Long
    ): Flow<Word> {
        val idFiled = "_id"
        val query = Query(
            Criteria().andOperator(
                Order::platform isEqualTo Platform.OPEN_SEA,
                Order::maker isEqualTo maker,
                Order::data /  OrderOpenSeaV1DataV1::nonce gte fromIncluding,
                Order::data /  OrderOpenSeaV1DataV1::nonce lt toExcluding,
            )
        )
        query.withHint(OrderRepositoryIndexes.BY_PLATFORM_MAKER_AND_NONCE.indexKeys)
        query.fields().include(idFiled)
        return template
            .find(query, Document::class.java, COLLECTION)
            .map { Word.apply(it.getString(idFiled)) }
            .asFlow()
    }

    override suspend fun findByTake(token: Address, tokenId: EthUInt256): Order? {
        val criteria = (Order::status isEqualTo OrderStatus.ACTIVE)
            .and(Order::take / Asset::type / NftAssetType::token).isEqualTo(token)
            .and(Order::take / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)
        return template.find(Query(criteria), Order::class.java).awaitFirstOrNull()
    }

    override fun findByTargetBalanceAndNotCanceled(maker: Address, token: Address): Flow<Order> {
        val criteria = Criteria.where("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(false)
            .and(Order::maker.name).isEqualTo(maker)
            .and(Order::cancelled.name).isEqualTo(false)
            .and("${Order::make.name}.${Asset::type.name}.${Erc20AssetType::token.name}").isEqualTo(token)

        val query = Query(criteria)
        return template.query<Order>().matching(query).all().asFlow()
    }

    override fun findAllBeforeLastUpdateAt(lastUpdatedAt: Date?): Flow<Order> {
        val criteria = Criteria()
            .run { lastUpdatedAt?.let { and(Order::lastUpdateAt).lte(it) } ?: this }
            .run { and(Order::cancelled).ne(true) }

        val queue = Query().addCriteria(criteria)

        queue.with(Sort.by(Sort.Direction.DESC, Order::lastUpdateAt.name))
        return template.query<Order>().matching(queue).all().asFlow()
    }

    fun findExpiredOrders(now: Instant): Flow<Order> {
        val query = Query(
            Criteria().andOperator(
                Order::status inValues listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE),
                Order::end exists true,
                Order::end gt 0,
                Order::end lt now.epochSecond
            )
        )
        return template.query<Order>().matching(query).all().asFlow()
    }

    fun findNotStartedOrders(now: Instant): Flow<Order> {
        val query = Query(
            Criteria().andOperator(
                Order::status isEqualTo OrderStatus.NOT_STARTED,
                Criteria().orOperator(
                    Criteria().orOperator(
                        Order::end exists false,
                        Order::end isEqualTo 0,
                    ),
                    Criteria().andOperator(
                        Order::end exists true,
                        Order::end gte now.epochSecond
                    )
                ),
                Criteria().orOperator(
                    Order::start exists false,
                    Criteria().andOperator(
                        Order::start exists true,
                        Order::start lte now.epochSecond
                    )
                )
            )
        )
        return template.query<Order>().matching(query).all().asFlow()
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

    companion object {
        const val COLLECTION = "order"
    }
}
