package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.util.Date

interface OrderRepository {
    /**
     * **This method should not be used in business code.** Order state is handled by `OrderUpdateService`.*
     *
     * To insert or update an order call `OrderUpdateService.save(OrderVersion)`.
     * Still there are possible use-cases for using this method: to update transient order's fields (`makeStock`) for example.
     * @see [com.rarible.protocol.order.core.service.OrderUpdateService]
     */

    suspend fun save(order: Order): Order

    suspend fun findById(hash: Word): Order?

    fun findAll(hashes: Collection<Word>): Flow<Order>

    fun findAll(platform: Platform, status: OrderStatus, fromHash: Word?): Flow<Order>

    suspend fun search(query: Query): List<Order>

    fun searchAll(query: Query): Flow<Order>

    suspend fun remove(hash: Word): Boolean

    fun findActive(): Flow<Order>

    fun findAll(): Flow<Order>

    fun findByTargetNftAndNotCanceled(maker: Address, token: Address, tokenId: EthUInt256): Flow<Order>

    fun findNonTerminateStatusSellOrdersByItemId(platform: Platform, token: Address, tokenId: EthUInt256): Flow<Order>

    fun findByTargetBalanceAndNotCanceled(maker: Address, token: Address): Flow<Order>

    fun findAllBeforeLastUpdateAt(lastUpdatedAt: Date?, status: OrderStatus?, platform: Platform?): Flow<Order>

    fun findMakeTypesOfBidOrders(
        token: Address,
        tokenId: EthUInt256,
        statuses: Collection<OrderStatus>
    ): Flow<AssetType>

    suspend fun findByMake(token: Address, tokenId: EthUInt256): Order?

    suspend fun findOpenSeaHashesByMakerAndByNonce(maker: Address, fromIncluding: Long, toExcluding: Long): Flow<Word>

    suspend fun findByTake(token: Address, tokenId: EthUInt256): Order?

    fun findTakeTypesOfSellOrders(
        token: Address,
        tokenId: EthUInt256,
        statuses: Collection<OrderStatus>
    ): Flow<AssetType>

    suspend fun createIndexes()

    suspend fun dropIndexes()

    fun findAllLiveBidsHashesLastUpdatedBefore(before: Instant): Flow<Word>

    fun findActiveSaleOrdersHashesByMakerAndToken(maker: Address, token: Address, platform: Platform): Flow<Order>

    fun findInActiveSaleOrdersHashesByMakerAndToken(maker: Address, token: Address, platform: Platform): Flow<Order>

    fun findByMakeAndByCounters(platform: Platform, maker: Address, counters: List<BigInteger>): Flow<Order>

    fun findLRByMakeAndByOrderCounters(maker: Address, counters: List<BigInteger>): Flow<Order>

    fun findLRByMakeAndBySubsetCounters(maker: Address, counters: List<BigInteger>): Flow<Order>

    fun findNotCanceledByMakerAndCounterLtThen(platform: Platform, maker: Address, counter: BigInteger): Flow<Word>

    fun findExpiredOrders(now: Instant): Flow<Order>

    fun findNotStartedOrders(now: Instant): Flow<Order>

    suspend fun findActiveBestSellOrdersOfCollection(token: Address, currency: Address, size: Int): List<Order>

    suspend fun findActiveSellCurrenciesByCollection(token: Address): List<Address>

    fun findNonTerminateOrdersByToken(token: Address): Flow<Order>
}
