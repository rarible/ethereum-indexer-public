package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.job.OrderStartEndCheckerJob
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.domain.AddressFactory
import java.time.Duration

@IntegrationTest
internal class OrderStartEndCheckerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var reactiveMongoTemplate: ReactiveMongoTemplate
    @Autowired
    private lateinit var  protocolOrderPublisher: ProtocolOrderPublisher
    @Autowired
    private lateinit var orderDtoConverter: OrderDtoConverter

    private val updaterJob
        get() = OrderStartEndCheckerJob(
            reactiveMongoTemplate,
            OrderListenerProperties(updateStatusByStartEndEnabled = true),
            orderDtoConverter,
            protocolOrderPublisher
        )

    @BeforeEach
    fun setup() {
        clearMocks(assetBalanceProvider)
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } coAnswers r@ {
            val asset = secondArg<Asset>()
            if (asset.type is EthAssetType) {
                return@r asset.value
            }
            return@r EthUInt256.ONE
        }
    }

    @Test
    fun `should make order expired`() = runBlocking<Unit> {
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
        val order = orderUpdateService.save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        // rewind end for matching expired query
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.ACTIVE)

        updaterJob.update(nowMillis())
        check(orderVersion.hash, OrderStatus.ENDED)
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
        val order = orderUpdateService.save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        updaterJob.update()
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
        coEvery { assetBalanceProvider.getAssetStock(any(), any()) } returns EthUInt256.ZERO
        val order = orderUpdateService.save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        updaterJob.update()
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
            end = null
        )
        val order = orderUpdateService.save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.NOT_STARTED)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("start", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.NOT_STARTED)

        updaterJob.update()
        check(orderVersion.hash, OrderStatus.ACTIVE)
    }

    @Test
    fun `should change order status if order is alive with only end`() = runBlocking<Unit> {
        val targetMaker = AddressFactory.create()
        val targetToken = AddressFactory.create()
        val make = Asset(Erc20AssetType(targetToken), EthUInt256.TEN)
        val take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.TEN)
        val orderVersion = createOrderVersion().copy(
            maker = targetMaker,
            make = make,
            take = take,
            start = null,
            end = nowMillis().plus(Duration.ofHours(2)).epochSecond
        )
        val order = orderUpdateService.save(orderVersion)
        assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)

        // rewind start for matching (start, end) interval
        mongo.updateMulti(Query(), Update().set("end", nowMillis().minus(Duration.ofMinutes(5)).epochSecond), MongoOrderRepository.COLLECTION).awaitFirst()
        check(orderVersion.hash, OrderStatus.ACTIVE)

        updaterJob.update()
        check(orderVersion.hash, OrderStatus.ENDED)
    }

    suspend fun check(hash: Word, status: OrderStatus) {
        val v = mongo.findById<OrderShort>(hash, MongoOrderRepository.COLLECTION).awaitFirst()
        assertThat(v.status).isEqualTo(status)
    }

    data class OrderShort(val status: OrderStatus)
}
