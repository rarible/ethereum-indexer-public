package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.CollectionAssetType
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc1155LazyAssetType
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Erc721LazyAssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.GenerativeArtAssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderPriceHistoryRecord
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.core.model.toOrderVersion
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.math.BigInteger

/**
 * ExchangeV2 on-chain order upsert test.
 */
@IntegrationTest
class ExchangeV2UpsertOrderDescriptorTest : AbstractExchangeV2Test() {

    @BeforeEach
    fun setUpBalances() {
        // It is necessary to make 'makePriceUsd/takePriceUsd/makeUsd/takeUsd' fields null.
        clearMocks(currencyApi)
        every { currencyApi.getCurrencyRate(any(), any(), any()) } returns Mono.empty()

        clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } coAnswers r@{
            val order = firstArg<Order>()
            if (order.make.type is EthAssetType) {
                return@r order.make.value
            }
            EthUInt256.TEN
        }
    }

    @Test
    fun `upsert on-chain order with make of ERC20 type`() = runBlocking<Unit> {
        val maker = userSender1.from()
        token1.mint(maker, BigInteger.TEN).execute().verifySuccess()
        val make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN)
        val take = Asset(Erc20AssetType(token2.address()), EthUInt256.ONE)
        val salt = EthUInt256.TEN
        val version = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = Order.hashKey(maker, make.type, take.type, salt.value)
        )
        `test insert order`(version, null)
    }

    @Test
    fun `upsert on-chain order with make of ETH type`() = runBlocking<Unit> {
        val makeValue = EthUInt256.TEN
        val make = Asset(EthAssetType, makeValue)
        val take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE)
        val maker = userSender1.from()
        val salt = EthUInt256.TEN
        val onChainOrder = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = Order.hashKey(maker, make.type, take.type, salt.value)
        )
        val remainingEth = BigInteger.valueOf(2)
        depositInitialBalance(userSender1.from(), makeValue.value.plus(remainingEth))
        `test insert order`(onChainOrder, makeValue)
        assertThat(getEthBalance(userSender1.from())).isEqualTo(remainingEth)
    }

    @Test
    fun `update on-chain order`() = runBlocking<Unit> {
        val makeValue = EthUInt256.TEN
        val make = Asset(EthAssetType, makeValue)
        val take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE)
        val maker = userSender1.from()
        val salt = EthUInt256.TEN
        val orderHash = Order.hashKey(maker, make.type, take.type, salt.value)
        val onChainOrder = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = orderHash
        )
        val newMakeValue = makeValue.plus(makeValue)
        depositInitialBalance(userSender1.from(), newMakeValue.value)

        `test insert order`(onChainOrder, makeValue)
        assertThat(orderRepository.findById(orderHash)?.make?.value).isEqualTo(makeValue)

        `test insert order`(
            onChainOrder = onChainOrder.copy(make = onChainOrder.make.copy(value = newMakeValue)),
            withSentEthValue = newMakeValue - makeValue,
            expectedSize = 2
        )
        assertThat(orderRepository.findById(orderHash)?.make?.value).isEqualTo(newMakeValue)

        assertThat(orderVersionRepository.findAllByHash(orderHash).count()).isEqualTo(2)
    }

    @Test
    fun `revert on-chain order`() = runBlocking<Unit> {
        val makeValue = EthUInt256.TEN
        val make = Asset(EthAssetType, makeValue)
        val take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE)
        val maker = userSender1.from()
        val salt = EthUInt256.TEN
        val orderHash = Order.hashKey(maker, make.type, take.type, salt.value)
        val onChainOrder = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = orderHash
        )
        val remainingEth = BigInteger.valueOf(2)
        depositInitialBalance(userSender1.from(), makeValue.value.plus(remainingEth))
        `test insert order`(onChainOrder, makeValue)
        assertThat(getEthBalance(userSender1.from())).isEqualTo(remainingEth)

        // Revert OnChainOrder log events.
        exchangeHistoryRepository.findLogEvents(orderHash, null).asFlow().collect { logEvent ->
            exchangeHistoryRepository.save(logEvent.copy(status = LogEventStatus.REVERTED)).awaitFirst()
        }
        orderUpdateService.update(orderHash)
        assertThat(orderVersionRepository.findAllByHash(orderHash).count()).isEqualTo(0)
        assertThat(orderRepository.findById(orderHash)).isNull()
    }

    @Test
    fun `create pending on-chain order`() = runBlocking<Unit> {
        val makeValue = EthUInt256.TEN
        val make = Asset(EthAssetType, makeValue)
        val take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE)
        val maker = userSender1.from()
        val salt = EthUInt256.TEN
        val orderHash = Order.hashKey(maker, make.type, take.type, salt.value)
        val onChainOrder = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = orderHash
        )
        val remainingEth = BigInteger.valueOf(2)
        val initialEthBalance = makeValue.value.plus(remainingEth)
        depositInitialBalance(userSender1.from(), initialEthBalance)
        `test insert order`(onChainOrder, makeValue)
        assertThat(getEthBalance(userSender1.from())).isEqualTo(remainingEth)

        // Make OnChainOrder log event as PENDING.
        exchangeHistoryRepository.findLogEvents(orderHash, null).asFlow().collect { logEvent ->
            exchangeHistoryRepository.save(logEvent.copy(
                status = LogEventStatus.PENDING,
                blockHash = null,
                blockNumber = null,
                logIndex = 0
            )).awaitFirst()
        }

        val order = orderRepository.findById(orderHash)
        assertThat(order).isNotNull; order!!
        orderRepository.remove(orderHash)
        orderUpdateService.update(orderHash)
        Wait.waitAssert {
            val pendingOrder = orderRepository.findById(orderHash)
            assertThat(pendingOrder).isNotNull; pendingOrder!!
            assertThat(pendingOrder.copy(version = 0)).isEqualTo(order.copy(
                pending = listOf(onChainOrder.copy(createdAt = order.createdAt, date = order.createdAt)),
                version = 0,
                createdAt = pendingOrder.createdAt
            ))
        }
    }

    @Test
    fun `cancel on-chain order`() = runBlocking<Unit> {
        val makeValue = EthUInt256.TEN
        val make = Asset(EthAssetType, makeValue)
        val take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE)
        val maker = userSender1.from()
        val salt = EthUInt256.TEN
        val orderHash = Order.hashKey(maker, make.type, take.type, salt.value)
        val onChainOrder = OnChainOrder(
            maker = maker,
            taker = null,
            make = make,
            take = take,
            orderType = OrderType.RARIBLE_V2,
            salt = salt,
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            platform = Platform.RARIBLE,
            priceUsd = null,
            hash = orderHash
        )
        val remainingEth = BigInteger.valueOf(2)
        val initialEthBalance = makeValue.value.plus(remainingEth)
        depositInitialBalance(userSender1.from(), initialEthBalance)
        `test insert order`(onChainOrder, makeValue)
        assertThat(getEthBalance(userSender1.from())).isEqualTo(remainingEth)

        exchange
            .cancel(onChainOrder.toOrderVersion().toOrderExactFields().forTx())
            .withSender(userSender1)
            .execute()
            .verifySuccess()

        // Check that the make ETH funds are returned to the seller.
        assertThat(getEthBalance(userSender1.from())).isEqualTo(initialEthBalance)

        Wait.waitAssert {
            val order = orderRepository.findById(orderHash)
            assertThat(order).isNotNull; order!!
            assertThat(order.cancelled).isTrue()
            assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
            assertThat(order.makeStock).isEqualTo(EthUInt256.ZERO)
        }
    }

    private suspend fun `test insert order`(
        onChainOrder: OnChainOrder,
        withSentEthValue: EthUInt256?,
        expectedSize: Int = 1
    ) {
        val orderVersion = onChainOrder.toOrderVersion()
        val upsertTimestamp = exchange.upsertOrder(orderVersion.toOrderExactFields().forTx())
            .withSender(userSender1)
            .let {
                if (withSentEthValue != null) {
                    it.withValue(withSentEthValue.value)
                } else {
                    it
                }
            }
            .execute()
            .verifySuccess()
            .getTimestamp()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ON_CHAIN_ORDER).collectList().awaitFirst()
            assertThat(items).hasSize(expectedSize)
            val gotOnChainOrder = items.last().data as OnChainOrder
            assertThat(gotOnChainOrder).isEqualTo(
                onChainOrder.copy(
                    createdAt = upsertTimestamp,
                    date = upsertTimestamp
                )
            )
        }

        Wait.waitAssert {
            fun OrderVersion.ignoreVersionId() = copy(id = ObjectId(0, 0))
            fun OrderVersion.ignoreOnChainOrderKey() = copy(onChainOrderKey = null)
            fun OrderVersion.ignore() = ignoreVersionId().ignoreOnChainOrderKey()

            val versions = orderVersionRepository.findAllByHash(onChainOrder.hash).toList(arrayListOf())
            assertThat(versions.size).isEqualTo(expectedSize)
            val version = versions.last()
            assertThat(version.ignore()).isEqualTo(
                orderVersion.ignore().copy(
                    createdAt = upsertTimestamp,
                    makePrice = version.makePrice,
                    takePrice = version.takePrice
                )
            )
        }

        Wait.waitAssert {
            val order = orderRepository.findById(onChainOrder.hash)
            assertThat(order).isNotNull; order!!

            fun decimals(type: AssetType): Int = when (type) {
                is Erc1155AssetType -> 0
                is Erc1155LazyAssetType -> 0
                is Erc20AssetType -> 18
                is GenerativeArtAssetType -> 0
                is Erc721AssetType -> 0
                is Erc721LazyAssetType -> 0
                is CryptoPunksAssetType -> 0
                is CollectionAssetType -> 0
                is EthAssetType -> 18
            }

            val expectedOrder = orderVersion.toOrderExactFields().copy(
                createdAt = upsertTimestamp,
                lastUpdateAt = upsertTimestamp,
                priceHistory = listOf(
                    OrderPriceHistoryRecord(
                        date = upsertTimestamp,
                        makeValue = orderVersion.make.value.value.toBigDecimal(decimals(orderVersion.make.type)),
                        takeValue = orderVersion.take.value.value.toBigDecimal(decimals(orderVersion.take.type))
                    )
                )
            )
            assertThat(order.copy(lastEventId = null)).isEqualTo(
                expectedOrder.copy(takePrice = order.takePrice, makePrice = order.makePrice)
            )
        }
    }
}
