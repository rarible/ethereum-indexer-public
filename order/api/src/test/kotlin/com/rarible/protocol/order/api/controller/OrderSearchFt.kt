package com.rarible.protocol.order.api.controller

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderSortDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.data.createErc20Asset
import com.rarible.protocol.order.api.data.createErc721Asset
import com.rarible.protocol.order.api.data.createErc721BidOrderVersion
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.model.AmmNftAssetType
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.stream.Stream
import com.rarible.protocol.order.core.data.randomOrder as createOrderFully

@IntegrationTest
class OrderSearchFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var poolHistoryRepository: PoolHistoryRepository

    companion object {

        @JvmStatic
        fun orders(): Stream<Arguments> = run {
            val now = nowMillis()

            Stream.of(
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_ASC,
                    listOf(
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(0)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    )
                ),
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_DESC,
                    listOf(
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully().copy(lastUpdateAt = now + Duration.ofMinutes(0))
                    )
                )
            )
        }

        @JvmStatic
        fun sellOrders(): Stream<Arguments> = run {
            val now = nowMillis()
            val make = createErc721Asset()
            val take = createErc20Asset()

            Stream.of(
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_ASC,
                    listOf(
                        createOrderFully(make = make, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(0)),
                        createOrderFully(make = make, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully(make = make, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully(make = make, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully(make = make, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    ),
                    listOf(
                        createOrderFully(make = take, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(0))
                    )
                ),
                Arguments.of(
                    OrderSortDto.LAST_UPDATE_DESC,
                    listOf(
                        createOrderFully().copy(make = make, take = take, lastUpdateAt = now + Duration.ofMinutes(4)),
                        createOrderFully().copy(make = make, take = take, lastUpdateAt = now + Duration.ofMinutes(3)),
                        createOrderFully().copy(make = make, take = take, lastUpdateAt = now + Duration.ofMinutes(2)),
                        createOrderFully().copy(make = make, take = take, lastUpdateAt = now + Duration.ofMinutes(1)),
                        createOrderFully().copy(make = make, take = take, lastUpdateAt = now + Duration.ofMinutes(0))
                    ),
                    listOf(
                        createOrderFully(make = take, take = take).copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    )
                )
            )
        }

        @JvmStatic
        fun orders4All(): Stream<Arguments> = run {
            val order: () -> Order = { createOrderFully().copy(makeStock = EthUInt256.ZERO) }
            val inactiveOrder = order()
            val activeOrder = order().copy(makeStock = EthUInt256.ONE).copy()
            val filledOrder = order().copy(fill = EthUInt256.TEN, take = Asset(EthAssetType, EthUInt256.TEN)).copy()
            val canceledOrder = order().copy(cancelled = true).copy()
            val orders = listOf(inactiveOrder, activeOrder, filledOrder, canceledOrder)
            Stream.of(
                Arguments.of(orders, inactiveOrder, listOf(OrderStatusDto.INACTIVE)),
                Arguments.of(orders, activeOrder, listOf(OrderStatusDto.ACTIVE)),
                Arguments.of(orders, filledOrder, listOf(OrderStatusDto.FILLED)),
                Arguments.of(orders, canceledOrder, listOf(OrderStatusDto.CANCELLED))
            )
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    fun `should find all orders with continuation`(
        sort: OrderSortDto,
        orders: List<Order>
    ) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val allOrders = mutableListOf<OrderDto>()

            var continuation: String? = null
            do {
                val result = orderClient.getOrdersAllByStatus(sort, continuation, 2, null).awaitFirst()
                assertThat(result.orders).hasSizeLessThanOrEqualTo(2)

                allOrders.addAll(result.orders)
                continuation = result.continuation
            } while (continuation != null)

            assertThat(allOrders).hasSize(orders.size)

            allOrders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("sellOrders")
    fun `should find all sell orders with sort`(
        sort: OrderSortDto,
        orders: List<Order>,
        other: List<Order>
    ) = runBlocking<Unit> {
        saveOrder(*(orders + other).shuffled().toTypedArray())

        Wait.waitAssert {
            val allOrders = mutableListOf<OrderDto>()

            var continuation: String? = null
            do {
                val result = orderClient.getSellOrdersByStatus(null, null, continuation, 2, null, sort).awaitFirst()
                assertThat(result.orders).hasSizeLessThanOrEqualTo(2)

                allOrders.addAll(result.orders)
                continuation = result.continuation
            } while (continuation != null)

            assertThat(allOrders).hasSize(orders.size)

            allOrders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("orders")
    fun `should find all orders`(
        sort: OrderSortDto,
        orders: List<Order>
    ) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val result = orderClient.getOrdersAllByStatus(sort, null, null, null).awaitFirst()
            assertThat(result.orders).hasSize(orders.size)

            result.orders.forEachIndexed { index, orderDto ->
                checkOrderDto(orderDto, orders[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("orders4All")
    fun `should find all orders by query`(
        orders: List<Order>, order: Order, statuses: List<OrderStatusDto>?
    ) = runBlocking<Unit> {
        saveOrder(*orders.shuffled().toTypedArray())

        Wait.waitAssert {
            val result = orderClient.getOrdersAllByStatus(null, null, 1, statuses).awaitFirst()
            assertThat(result.orders.size).isEqualTo(1)
            assertThat(order.hash).isEqualTo(result.orders[0].hash)
        }
    }

    @Test
    fun `should find with SellOrdersByItemAndByStatus`() = runBlocking<Unit> {
        val order1 = createOrderFully()
        val order2 = createOrderFully().copy(
            make = Asset(
                Erc721AssetType(AddressFactory.create(), EthUInt256.ONE),
                EthUInt256.TEN
            )
        )
        saveOrder(order1, order2)

        Wait.waitAssert {
            val result = orderClient.getSellOrdersByItemAndByStatus(
                order2.make.type.token.toString(),
                order2.make.type.tokenId?.value.toString(),
                order2.maker.toString(),
                null,
                null,
                null, 1, null, null
            ).awaitFirst()
            assertThat(result.orders.size).isEqualTo(1)
        }
    }

    @Test
    fun `should find sell-orders by item, currency and sort asc`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1 = createOrderFully().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(1)),
            makePrice = BigDecimal.valueOf(1L)
        )
        val order2 = createOrderFully().copy(
            maker = order1.maker,
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(2)),
            makePrice = BigDecimal.valueOf(2L)
        )
        val order3 = createOrderFully().copy(
            maker = order1.maker,
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(1)),
            makePrice = BigDecimal.valueOf(1L)
        )
        saveOrder(order3, order2, order1)

        val result = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            null,
            null, 1, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].take.value).isEqualTo(1)

        val result2 = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            null,
            result.continuation, 2, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].take.value).isEqualTo(2)
        assertThat(result2.continuation).isNull()
    }

    @Test // TODO revisit in PT-1652
    fun `should find amm sell-orders by item and currency`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val tokenId = EthUInt256.TEN
        val currencyToken = AddressFactory.create()
        val ammOrder = createOrderFully().copy(
            make = Asset(AmmNftAssetType(token), EthUInt256.ONE),
            take = Asset(Erc20AssetType(currencyToken), EthUInt256.of(1)),
            makePrice = BigDecimal.valueOf(1L)
        )

        val poolCreate = randomSellOnChainAmmOrder().copy(
            collection = token,
            tokenIds = listOf(tokenId),
            currency = currencyToken,
            hash = ammOrder.hash
        )
        val logEvent = ReversedEthereumLogRecord(
            id = ObjectId().toHexString(),
            data = poolCreate,
            address = randomAddress(),
            topic = Word.apply(RandomUtils.nextBytes(32)),
            transactionHash = randomWord(),
            index = RandomUtils.nextInt(),
            minorLogIndex = 0,
            status = EthereumBlockStatus.CONFIRMED
        )
        poolHistoryRepository.save(logEvent).awaitFirst()

        saveOrder(ammOrder)

        val result = orderClient.getSellOrdersByItemAndByStatus(
            token.prefixed(),
            tokenId.value.toString(),
            null,
            null,
            PlatformDto.SUDOSWAP,
            null, 1, null, currencyToken.hex()
        ).awaitFirst()

        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].hash).isEqualTo(ammOrder.hash)
        assertThat(result.continuation).isNull()

        val result2 = orderClient.getSellOrdersByItemAndByStatus(
            token.prefixed(),
            tokenId.value.toString(),
            null,
            null,
            PlatformDto.SUDOSWAP,
            null, 1, null, randomAddress().prefixed() //other currency
        ).awaitFirst()

        assertThat(result2.orders.size).isEqualTo(0)
        assertThat(result2.continuation).isNull()
    }

    @Test
    fun `should find bid-orders by currency and item and sorted desc`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(1)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(1L)
        )
        val order2V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(2)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(2L)
        )
        reduceOrder(order1V, order2V)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            listOf(order1V.maker, order2V.maker),
            null,
            null,
            null,
            1,
            OrderStatusDto.values().toList(),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(2)

        val result2 = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            listOf(order1V.maker, order2V.maker),
            null,
            null,
            result.continuation,
            2,
            OrderStatusDto.values().toList(),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].make.value).isEqualTo(1)
        assertThat(result2.continuation).isNull()
    }

    @Test
    fun `should find best bid from versions with same take price`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()

        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.ONE),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(1L),
            // Imitating ordering by ID
            id = ObjectId(Date(nowMillis().toEpochMilli() - 1000))
        )

        val order2V = createErc721BidOrderVersion().copy(
            make = order1V.make,
            take = order1V.take,
            takePrice = order1V.takePrice
        )

        val (savedOrdersV1, savedOrder2V) = reduceOrder(order1V, order2V)

        // orderV1 should be associated with existing active order to be retrieved as ACTIVE
        val order1 = createOrderFully().copy(
            id = savedOrdersV1.id,
            hash = savedOrdersV1.id.hash,
            version = savedOrder2V.version
        )
        orderRepository.save(order1)

        // Ensure for all statuses we got "historical" order only
        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            null,
            null,
            null,
            null,
            1,
            OrderStatusDto.values().toList(),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].taker).isEqualTo(order2V.taker)

        // But for ACTIVE order1 should be returned (he saved before order2, so its _id should be second for DESC)
        val result2 = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            null,
            null,
            null,
            null,
            1,
            listOf(OrderStatusDto.ACTIVE),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].taker).isEqualTo(order1V.taker)
    }

    @Test
    fun `should find bid-orders by maker and sorted desc`() = runBlocking<Unit> {
        val maker = AddressFactory.create()
        val maker2 = AddressFactory.create()
        val makeAddress = AddressFactory.create()
        val currencyToken1 = AddressFactory.create()
        val currencyToken2 = AddressFactory.create()
        val now = nowMillis()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken1), EthUInt256.of(1)),
            maker = maker,
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(1L),
            createdAt = now.minusSeconds(5)
        )
        val order2V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken1), EthUInt256.of(2)),
            maker = maker,
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(2L),
            createdAt = now
        )
        val order3V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken2), EthUInt256.of(2)),
            maker = maker2,
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(2L),
            createdAt = now
        )
        reduceOrder(order1V, order2V, order3V)

        val result = orderClient.getOrderBidsByMakerAndByStatus(
            listOf(maker),
            null,
            null,
            null,
            1,
            OrderStatusDto.values().toList(),
            null,
            null,
            null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(2)
        assertThat(result.orders[0].maker).isEqualTo(maker)
        assertThat(result.continuation).isNotNull

        val result2 = orderClient.getOrderBidsByMakerAndByStatus(
            listOf(order1V.maker, order2V.maker),
            null,
            null,
            result.continuation,
            2,
            OrderStatusDto.values().toList(),
            null,
            null,
            null
        ).awaitFirst()
        assertThat(result2.orders.size).isEqualTo(1)
        assertThat(result2.orders[0].make.value).isEqualTo(1)
        assertThat(result2.continuation).isNull()

        val result3 = orderClient.getOrderBidsByMakerAndByStatus(
            listOf(maker, maker2),
            null,
            null,
            null,
            10,
            OrderStatusDto.values().toList(),
            null,
            null,
            null
        ).awaitFirst()
        assertThat(result3.orders.size).isEqualTo(3)
        assertThat(result3.orders.map { it.maker }.toSet()).containsExactlyInAnyOrder(maker, maker2)

        val result4 = orderClient.getOrderBidsByMakerAndByStatus(
            listOf(maker, maker2),
            null,
            null,
            null,
            10,
            OrderStatusDto.values().toList(),
            listOf(currencyToken2),
            null,
            null
        ).awaitFirst()
        assertThat(result4.orders.map { it.id }.toSet()).containsExactlyInAnyOrder(order3V.hash.toString())
    }

    @Test
    fun `should find sell-orders by ETH currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = Address.ZERO()
        val order1 = createOrderFully().copy(
            make = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            take = Asset(EthAssetType, EthUInt256.of(123)),
            makePrice = BigDecimal.valueOf(123L)
        )
        saveOrder(order1)

        val result = orderClient.getSellOrdersByItemAndByStatus(
            order1.make.type.token.toString(),
            order1.make.type.tokenId?.value.toString(),
            order1.maker.toString(),
            null,
            null,
            null, 1, null, currencyToken.hex()
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].take.value).isEqualTo(123)
    }

    @Test
    fun `should find bid-order by ETH currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = Address.ZERO()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(EthAssetType, EthUInt256.of(123)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(123L)
        )
        reduceOrder(order1V)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            listOf(order1V.maker),
            null,
            null,
            null,
            1,
            OrderStatusDto.values().toList(),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(123)
    }

    @Test
    fun `should find bid-order by currency`() = runBlocking<Unit> {
        val makeAddress = AddressFactory.create()
        val currencyToken = AddressFactory.create()
        val order1V = createErc721BidOrderVersion().copy(
            make = Asset(Erc20AssetType(currencyToken), EthUInt256.of(123)),
            take = Asset(Erc721AssetType(makeAddress, EthUInt256.ONE), EthUInt256.TEN),
            takePrice = BigDecimal.valueOf(123L)
        )
        reduceOrder(order1V)
        val order1VEth = createErc721BidOrderVersion().copy(
            maker = order1V.maker,
            make = Asset(EthAssetType, EthUInt256.of(123)),
            take = order1V.take,
            takePrice = BigDecimal.valueOf(123L)
        )
        reduceOrder(order1VEth)

        val result = orderClient.getOrderBidsByItemAndByStatus(
            order1V.take.type.token.toString(),
            order1V.take.type.tokenId?.value.toString(),
            listOf(order1V.maker),
            null,
            null,
            null,
            2,
            OrderStatusDto.values().toList(),
            currencyToken.hex(),
            null,
            null
        ).awaitFirst()
        assertThat(result.orders.size).isEqualTo(1)
        assertThat(result.orders[0].make.value).isEqualTo(123)
    }

    @Test
    fun `should find multiple on-chain bids with the same hash`() = runBlocking<Unit> {
        val maker = randomAddress()
        val make = Asset(EthAssetType, EthUInt256.TEN)
        val contract = randomAddress()
        val take = Asset(CryptoPunksAssetType(contract, EthUInt256.of(42)), EthUInt256.ONE)
        val salt = BigInteger.ZERO
        val orderHash = Order.hashKey(maker, make.type, take.type, salt)
        val numberOfBids = 105 // Bigger than the default page size (50).
        val bidPrices = (1..numberOfBids).map { randomInt(1, 1_000_000) }
        val orderVersions = bidPrices.map { bidPrice ->
            val takePrice = bidPrice.toBigInteger().toBigDecimal(18)
            // TODO: if we set 'takePriceUsd = null' for all bids, then the bug https://rarible.atlassian.net/browse/RPN-1414
            //  will incorrectly return only some (not all) subsequent order versions having the same price.
            val takePriceUsd = takePrice.multiply(BigDecimal.valueOf(4500))
            OrderVersion(
                maker = maker,
                taker = randomAddress(),
                make = make.copy(value = EthUInt256.of(bidPrice)),
                take = take,
                createdAt = nowMillis(),
                platform = Platform.RARIBLE,
                type = OrderType.RARIBLE_V2,
                salt = EthUInt256(salt),
                start = null,
                end = Instant.now().plusSeconds(1000).epochSecond,
                data = OrderRaribleV2DataV1(emptyList(), emptyList()),
                signature = null,
                makePriceUsd = null,
                takePriceUsd = takePriceUsd,
                makePrice = null,
                takePrice = takePrice,
                makeUsd = null,
                takeUsd = null,
                hash = orderHash
            )
        }
        // Mock the available balance of these orders.
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } answers {
            MakeBalanceState(firstArg<Order>().make.value)
        }
        reduceOrder(*orderVersions.toTypedArray())
        val bids = orderClient.getOrderBidsByItemAndByStatus(
            contract.prefixed(),
            take.type.tokenId?.value.toString(),
            listOf(maker),
            null,
            null,
            null,
            null,
            listOf(OrderStatusDto.ACTIVE),
            null,
            null,
            null
        ).awaitFirst()

        // Only the bid with the highest price must be returned with status ACTIVE (irrespective of the request window size)
        assertThat(bids.orders.map { it.status!! to it.make.value }).isEqualTo(
            listOf(OrderStatusDto.ACTIVE to bidPrices.maxOrNull()!!.toBigInteger())
        )
    }

    private fun checkOrderDto(orderDto: OrderDto, order: Order) {
        assertThat(orderDto.hash).isEqualTo(order.hash)
    }

    private suspend fun saveOrder(vararg order: Order) {
        order.forEach { orderRepository.save(it) }
    }

    private suspend fun reduceOrder(vararg order: OrderVersion): List<Order> {
        return order.map { save(it) }
    }
}
