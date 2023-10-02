package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.misc.isSingleton
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.EXPIRED_BID_STATUSES
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes.SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes.SELL_ORDERS_BY_CURRENCY_COLLECTION_DEFINITION
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.KPropertyPath
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
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.util.Date

@Component
class MongoOrderRepository(
    private val template: ReactiveMongoOperations
) : OrderRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun dropIndexes() {
        dropIndexes(
            "make.type.token_1_make.type.tokenId_1_lastUpdateAt_1__id_1",
            "end_1_start_1_makeStock_1__id_1",
            "make.type.nft_1_createdAt_1__id_1", // Incorrect SELL_ORDERS_DEFINITION
            "make.type.nft_1_platform_1_createdAt_1__id_1", // Incorrect SELL_ORDERS_PLATFORM_DEFINITION
            "platform_1_maker_1_data.counter_1_status_1",
            "makeStock_-1_lastUpdateAt_-1",
            "makeStock_-1_lastUpdateAt_-1__id_1",
            "makeStock_1_lastUpdateAt_1__id_1",
            "platform_1_lastUpdateAt_1__id_1",
            "platform_1_maker_1_data.val com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1.nonce: kotlin.Long?_1",
        )
    }

    override suspend fun createIndexes() {
        OrderRepositoryIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    override suspend fun save(order: Order): Order {
        return template.save(order.withDbUpdated()).awaitFirst()
    }

    override suspend fun findById(hash: Word): Order? {
        return template.findById<Order>(Order.Id(hash)).awaitFirstOrNull()
    }

    override fun findAll(hashes: Collection<Word>): Flow<Order> {
        val criteria = Criteria.where("_id").inValues(hashes.map { Order.Id(it) })
        return template.find<Order>(Query.query(criteria)).asFlow()
    }

    override suspend fun search(query: Query): List<Order> {
        return template.query<Order>()
            .matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }

    override fun searchAll(query: Query): Flow<Order> {
        return template.query<Order>().matching(query).all().asFlow()
    }

    override suspend fun remove(hash: Word): Boolean {
        val criteria = Criteria.where("_id").isEqualTo(Order.Id(hash))
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
                        if (fromHash != null) Order::id gt Order.Id(fromHash) else null
                    )
                )
            ).withHint(OrderRepositoryIndexes.BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION.indexKeys)
                .with(Sort.by(Order::status.name, Order::lastUpdateAt.name, "_id"))
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

    override fun findSellOrdersNotCancelledByItemId(
        platform: Platform,
        token: Address,
        tokenId: EthUInt256
    ): Flow<Order> {
        val criteria =
            (Order::make / Asset::type / NftAssetType::nft isEqualTo true)
                .and(Order::cancelled).isEqualTo(false)
                .and(Order::platform).isEqualTo(platform)
                .and(Order::make / Asset::type / NftAssetType::token).isEqualTo(token)
                .and(Order::make / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)

        val query = Query(criteria)
            .withHint(OrderRepositoryIndexes.SELL_ORDERS_BY_ITEM_PLATFORM_NOT_CANCELLED.indexKeys)
        return template.query<Order>().matching(query).all().asFlow()
    }

    override fun findTakeTypesOfSellOrders(
        token: Address,
        tokenId: EthUInt256,
        statuses: Collection<OrderStatus>
    ): Flow<AssetType> {
        // TODO ideally refactor it and bidCurrencies to have separate requests for collection orders
        val criteria = Criteria().andOperator(
            Order::status inValues statuses,
            Order::make / Asset::type / NftAssetType::token isEqualTo token,
            Order::make / Asset::type / NftAssetType::tokenId isEqualTo tokenId,
        )
        return template.findDistinct(
            Query(criteria),
            "${Order::take.name}.${Asset::type.name}",
            Order::class.java,
            AssetType::class.java
        ).asFlow()
    }

    override fun findMakeTypesOfBidOrders(
        token: Address,
        tokenId: EthUInt256,
        statuses: Collection<OrderStatus>
    ): Flow<AssetType> {
        val criteria = Criteria().andOperator(
            Order::status inValues statuses,
            Order::take / Asset::type / NftAssetType::token isEqualTo token,
            Criteria().orOperator(
                Order::take / Asset::type / NftAssetType::tokenId isEqualTo tokenId,
                (Order::take / Asset::type / NftAssetType::tokenId exists false)
                    .and(Order::take / Asset::type / NftAssetType::nft).isEqualTo(true)
            )
        )
        return template.findDistinct(
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
                Order::status ne OrderStatus.CANCELLED,
                Order::platform isEqualTo Platform.OPEN_SEA,
                Order::maker isEqualTo maker,
                Order::data / OrderOpenSeaV1DataV1::nonce gte fromIncluding,
                Order::data / OrderOpenSeaV1DataV1::nonce lt toExcluding,
            )
        )
        query.fields().include(idFiled)
        return template
            .find(query, Document::class.java, COLLECTION)
            .map { it.getString(idFiled).toOrderId().hash }
            .asFlow()
    }

    override fun findNotCanceledByMakerAndCounterLtThen(
        platform: Platform,
        maker: Address,
        counter: BigInteger
    ): Flow<Word> {
        val idFiled = "_id"
        val counterValue = EthUInt256.of(counter)

        val query = Query(
            Criteria().and(Order::platform.name).isEqualTo(platform)
                .and(Order::maker.name).isEqualTo(maker)
                .and(Order::status.name).ne(OrderStatus.CANCELLED)
                .and(COUNTER_HEX_KEY).exists(true).lt(counterValue)
        )
        query.withHint(OrderRepositoryIndexes.BY_PLATFORM_MAKER_GLOBAL_COUNTER_STATUS.indexKeys)
        query.fields().include(idFiled)
        return template
            .find(query, Document::class.java, COLLECTION)
            .map { it.getString(idFiled).toOrderId().hash }
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

    override fun findAllBeforeLastUpdateAt(
        lastUpdatedAt: Date?,
        status: OrderStatus?,
        platform: Platform?
    ): Flow<Order> {
        val criteria = Criteria()
            .run { lastUpdatedAt?.let { and(Order::lastUpdateAt).lte(it) } ?: this }
            .run { status?.let { and(Order::status).isEqualTo(it) } ?: this }
            .run { platform?.let { and(Order::platform).isEqualTo(it) } ?: this }
            .run { and(Order::cancelled).ne(true) }

        val queue = Query().addCriteria(criteria)

        queue.with(Sort.by(Sort.Direction.DESC, Order::lastUpdateAt.name))
        return template.query<Order>().matching(queue).all().asFlow()
    }

    override fun findAllLiveBidsHashesLastUpdatedBefore(before: Instant): Flow<Word> {
        val idFiled = "_id"
        val criteria = (Order::take / Asset::type / AssetType::nft isEqualTo true)
            .and(Order::platform).isEqualTo(Platform.RARIBLE)
            .and(Order::status).`in`(EXPIRED_BID_STATUSES)
            .and(Order::lastUpdateAt).lte(before)

        val query = Query.query(criteria)
            .withHint(OrderRepositoryIndexes.BY_BID_PLATFORM_STATUS_LAST_UPDATED_AT.indexKeys)
        query.fields().include(idFiled)
        return template.find(query, Document::class.java, COLLECTION)
            .map { it.getString(idFiled).toOrderId().hash }
            .asFlow()
    }

    override fun findExpiredOrders(now: Instant): Flow<Order> {
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

    override suspend fun findActiveSellCurrenciesByCollection(token: Address): List<Address> {
        val query = Query(
            Criteria().andOperator(
                (Order::make / Asset::type / NftAssetType::token).isEqualTo(token),
                (Order::make / Asset::type / NftAssetType::nft).isEqualTo(true),
                Order::status isEqualTo OrderStatus.ACTIVE,
            )
        ).withHint(SELL_ORDERS_BY_CURRENCY_COLLECTION_DEFINITION.indexKeys)
        return template.findDistinct<Address>(
            query,
            "${Order::take.name}.${Asset::type.name}.${Erc20AssetType::token.name}",
            Order::class.java,
            Address::class.java
        ).collectList().awaitFirst()
    }

    override fun findNotStartedOrders(now: Instant): Flow<Order> {
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

    override suspend fun findActiveBestSellOrdersOfCollection(
        token: Address,
        currency: Address,
        size: Int
    ): List<Order> {
        val query = Query(
            Criteria().andOperator(
                (Order::make / Asset::type / NftAssetType::nft).isEqualTo(true),
                (Order::status).isEqualTo(OrderStatus.ACTIVE),
                (Order::make / Asset::type / NftAssetType::token).isEqualTo(token),
                (Order::take / Asset::type / NftAssetType::token).isEqualTo(currency),
            )
        ).with(
            Sort.by(Sort.Direction.ASC, Order::makePrice.name, "_id")
        ).withHint(
            SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION.indexKeys
        ).limit(size)

        return template.find(query, Order::class.java).collectList().awaitFirst()
    }

    override fun findActiveSaleOrdersHashesByMakerAndToken(
        maker: Address,
        token: Address,
        platform: Platform
    ) = findSaleOrdersHashesByMakerAndTokenAndStatus(maker, token, platform, OrderStatus.ACTIVE)

    override fun findInActiveSaleOrdersHashesByMakerAndToken(
        maker: Address,
        token: Address,
        platform: Platform
    ) = findSaleOrdersHashesByMakerAndTokenAndStatus(maker, token, platform, OrderStatus.INACTIVE)

    private fun findSaleOrdersHashesByMakerAndTokenAndStatus(
        maker: Address,
        token: Address,
        platform: Platform,
        status: OrderStatus
    ): Flow<Order> {
        val criteria = where(Order::maker).isEqualTo(maker)
            .and(Order::status).isEqualTo(status)
            .and(Order::make / AssetType::type / AssetType::nft).isEqualTo(true)
            .and(Order::make / AssetType::type / NftAssetType::token).isEqualTo(token)
            .and(Order::platform).isEqualTo(platform)

        val query = Query(criteria).withHint(OrderRepositoryIndexes.BY_MAKER_AND_STATUS_ONLY_SALE_ORDERS.indexKeys)
        return template.find(query, Order::class.java, COLLECTION).asFlow()
    }

    override fun findByMakeAndByCounters(platform: Platform, maker: Address, counters: List<BigInteger>): Flow<Order> {
        return findByMakeAndCounters(
            platform = platform,
            maker = maker,
            counters = counters,
            counterKey = Order::data / OrderLooksrareDataV2::counterHex,
            hint = OrderRepositoryIndexes.BY_PLATFORM_MAKER_GLOBAL_COUNTER_STATUS
        )
    }

    override fun findLRByMakeAndByOrderCounters(maker: Address, counters: List<BigInteger>): Flow<Order> {
        return findByMakeAndCounters(
            platform = Platform.LOOKSRARE,
            maker = maker,
            counters = counters,
            counterKey = Order::data / OrderLooksrareDataV2::orderNonce,
            hint = OrderRepositoryIndexes.BY_PLATFORM_MAKER_ORDER_COUNTER_STATUS
        )
    }

    override fun findLRByMakeAndBySubsetCounters(maker: Address, counters: List<BigInteger>): Flow<Order> {
        return findByMakeAndCounters(
            platform = Platform.LOOKSRARE,
            maker = maker,
            counters = counters,
            counterKey = Order::data / OrderLooksrareDataV2::subsetNonce,
            hint = OrderRepositoryIndexes.BY_PLATFORM_MAKER_SUBSET_COUNTER_STATUS
        )
    }

    private fun findByMakeAndCounters(
        platform: Platform,
        maker: Address,
        counters: List<BigInteger>,
        counterKey: KPropertyPath<EthUInt256, OrderData>,
        hint: IndexDefinition
    ): Flow<Order> {
        val counterValues = counters.map { EthUInt256.of(it) }
        val criteria = where(Order::platform).isEqualTo(platform)
            .and(Order::maker).isEqualTo(maker)
            .run {
                if (counterValues.isSingleton) {
                    and(counterKey).isEqualTo(counterValues.single())
                } else {
                    and(counterKey).inValues(counterValues)
                }
            }

        val query = Query(criteria).withHint(hint.indexKeys)
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
        const val COUNTER_HEX_KEY = "data.counterHex"
    }
}
