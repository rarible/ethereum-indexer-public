package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.StartEndWorkerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.AddressFactory
import java.time.Duration
import java.time.Instant

@IntegrationTest
@ExperimentalCoroutinesApi
internal class OrderStartEndCheckerHandlerIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: OrderStartEndCheckerHandler

    @Autowired
    private lateinit var properties: StartEndWorkerProperties

    @BeforeEach
    fun setup() {
        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } coAnswers r@{
            val asset = secondArg<Asset>()
            if (asset.type is EthAssetType) {
                return@r MakeBalanceState(asset.value)
            }
            return@r MakeBalanceState(EthUInt256.ONE)
        }
    }

    @Test
    fun `make expired - sell order`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().minus(Duration.ofHours(1)).epochSecond,
            end = nowMillis().plus(Duration.ofHours(1)).epochSecond
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        // rewind end for matching expired query
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.ACTIVE)

        val updateTime = nowMillis()
        handler.update(updateTime)
        assertThat(check(orderVersion.hash, OrderStatus.ENDED)).isEqualTo(updateTime)
    }

    @Test
    fun `expired - ok, in advance`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = null,
            end = nowMillis().plus(properties.cancelOffset.dividedBy(2)).epochSecond
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        check(orderVersion.hash, OrderStatus.ACTIVE)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ENDED)
    }

    @Test
    fun `make expired - bid order`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().minus(Duration.ofHours(1)).epochSecond,
            end = nowMillis().plus(Duration.ofHours(1)).epochSecond
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        // rewind end for matching expired query
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.ACTIVE)

        val updateTime = nowMillis()
        handler.update(updateTime)
        assertThat(check(orderVersion.hash, OrderStatus.ENDED)).isEqualTo(updateTime)
    }

    @Test
    fun `should make order expired if it is already inactive`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().minus(Duration.ofHours(1)).epochSecond,
            end = nowMillis().plus(Duration.ofHours(1)).epochSecond
        )
        val order = save(orderVersion)
        val updated = mongo.save(order.copy(makeStock = EthUInt256.ZERO)).awaitSingle()
        assertThat(updated.status).isEqualTo(OrderStatus.INACTIVE)

        // rewind end for matching expired query
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()

        handler.handle()
        check(orderVersion.hash, OrderStatus.ENDED)
    }

    @Test
    fun `should make order expired if it is already inactive and end = 0`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().minus(Duration.ofHours(1)).epochSecond,
            end = 0
        )
        val order = save(orderVersion)
        val updated = mongo.save(order.copy(makeStock = EthUInt256.ZERO)).awaitSingle()
        assertThat(updated.status).isEqualTo(OrderStatus.INACTIVE)

        handler.handle()
        check(orderVersion.hash, OrderStatus.INACTIVE)
    }

    @Test
    fun `should change order status to active if order is alive`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().plus(Duration.ofHours(1)).epochSecond,
            end = nowMillis().plus(Duration.ofHours(2)).epochSecond
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ACTIVE)
    }

    @Test
    fun `should change order status to active if end = 0`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().plus(Duration.ofHours(1)).epochSecond,
            end = 0
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ACTIVE)
    }

    @Test
    fun `shouldn change order status to inactive due to makeStock = 0`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().plus(Duration.ofHours(1)).epochSecond,
            end = nowMillis().plus(Duration.ofHours(2)).epochSecond
        )
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns MakeBalanceState(EthUInt256.ZERO)
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        handler.handle()
        check(orderVersion.hash, OrderStatus.INACTIVE)
    }

    @Test
    fun `should change order status if order is alive with only start`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().plus(Duration.ofHours(1)).epochSecond,
            end = Instant.now().plusSeconds(1000).epochSecond,
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ACTIVE)
    }

    @Test
    fun `should change order status if order is alive with end = 0`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = nowMillis().plus(Duration.ofHours(1)).epochSecond,
            end = 0
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ACTIVE)
    }

    @Test
    fun `should change order status if order is alive with only end`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = null,
            end = nowMillis().plus(Duration.ofHours(2)).epochSecond
        )
        val order = save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.ACTIVE)

        handler.handle()
        check(orderVersion.hash, OrderStatus.ENDED)
    }

    suspend fun check(hash: Word, status: OrderStatus): Instant {
        val savedOrder = mongo.findById<OrderShort>(hash, MongoOrderRepository.COLLECTION).awaitFirst()

        assertThat(savedOrder.status).isEqualTo(status)
        assertThat(savedOrder.cancelled).isEqualTo(savedOrder.status == OrderStatus.CANCELLED)
        return savedOrder.lastUpdateAt
    }

    data class OrderShort(val status: OrderStatus, val lastUpdateAt: Instant, val cancelled: Boolean)
}
