package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.math.BigDecimal
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
    fun `upsert on-chain order with make of ERC20 type`() = runBlocking {
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
        `test insert order`(version)
    }

    @Test
    fun `upsert on-chain order with make of ETH type`() = runBlocking {
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
        depositInitialBalance(userSender1.from(), makeValue.value.plus(BigInteger.valueOf(2)))
        `test insert order`(onChainOrder)
        assertEquals(BigInteger.valueOf(2), getEthBalance(userSender1.from()))
    }

    private suspend fun `test insert order`(onChainOrder: OnChainOrder) {
        val orderVersion = with(onChainOrder) {
            OrderVersion(
                maker = maker,
                taker = taker,
                make = make,
                take = take,
                start = start,
                end = end,
                data = data,
                createdAt = createdAt,
                salt = salt,
                onChainOrderKey = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makePrice = null,
                takePrice = null,
                makeUsd = null,
                takeUsd = null
            )
        }

        val upsertTimestamp = exchange.upsertOrder(orderVersion.toOrderExactFields().forTx())
            .withSender(userSender1)
            .let {
                if (onChainOrder.make.type is EthAssetType) {
                    it.withValue(onChainOrder.make.value.value)
                } else {
                    it
                }
            }
            .execute()
            .verifySuccess()
            .getTimestamp()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ON_CHAIN_ORDER).collectList().awaitFirst()
            Assertions.assertThat(items).hasSize(1)
            val gotOnChainOrder = items.single().data as OnChainOrder
            assertEquals(onChainOrder.copy(createdAt = upsertTimestamp, date = upsertTimestamp), gotOnChainOrder)
        }

        Wait.waitAssert {
            fun OrderVersion.ignoreVersionId() = copy(id = ObjectId(0, 0))
            fun OrderVersion.ignoreOnChainOrderKey() = copy(onChainOrderKey = null)
            fun OrderVersion.ignore() = ignoreVersionId().ignoreOnChainOrderKey()

            val versions = orderVersionRepository.findAllByHash(onChainOrder.hash).toList(arrayListOf())
            assertEquals(1, versions.size)
            val version = versions.single()
            assertEquals(
                orderVersion.ignore().copy(createdAt = upsertTimestamp, makePrice = version.makePrice, takePrice = version.takePrice),
                version.ignore()
            )
        }

        Wait.waitAssert {
            val order = orderRepository.findById(onChainOrder.hash)
            assertNotNull(order)

            fun decimals(type: AssetType): Int = when (type) {
                is Erc1155AssetType -> 0
                is Erc1155LazyAssetType -> 0
                is Erc20AssetType -> 18
                is GenerativeArtAssetType -> 0
                is Erc721AssetType -> 0
                is Erc721LazyAssetType -> 0
                is CryptoPunksAssetType -> 0
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
            assertEquals(expectedOrder.copy(takePrice = order?.takePrice, makePrice = order?.makePrice), order?.copy(lastEventId = null))
        }
    }
}
