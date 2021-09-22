package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Order
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address
import java.util.*

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

    suspend fun search(query: Query): List<Order>

    suspend fun remove(hash: Word): Boolean

    fun findActive(): Flow<Order>

    fun findAll(): Flow<Order>

    fun findByTargetNftAndNotCanceled(maker: Address, token: Address, tokenId: EthUInt256): Flow<Order>

    fun findByTargetBalanceAndNotCanceled(maker: Address, token: Address): Flow<Order>

    fun findAllBeforeLastUpdateAt(lastUpdatedAt: Date?): Flow<Order>

    suspend fun createIndexes()
}

