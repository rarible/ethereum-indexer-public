package com.rarible.protocol.order.listener.service.descriptors.exchange.v2

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

@IntegrationTest
class ExchangeV2UpsertOrderDescriptorTest : AbstractExchangeV2Test() {
    @Test
    fun `upsert on-chain order with make of ERC20 type`() = runBlocking {
        token1.mint(userSender1.from(), BigInteger.TEN).execute().verifySuccess()
        val version = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(Erc20AssetType(token1.address()), EthUInt256.TEN),
            take = Asset(Erc20AssetType(token2.address()), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = 1,
            end = 2,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.RARIBLE
        )
        `test insert order`(version)
    }

    @Test
    fun `upsert on-chain order with make of ETH type`() = runBlocking {
        val makeValue = EthUInt256.TEN
        val version = OrderVersion(
            maker = userSender1.from(),
            taker = null,
            make = Asset(EthAssetType, makeValue),
            take = Asset(Erc721AssetType(token721.address(), EthUInt256.ONE), EthUInt256.ONE),
            type = OrderType.RARIBLE_V2,
            salt = EthUInt256.TEN,
            start = 1,
            end = 2,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makeUsd = null,
            takeUsd = null,
            platform = Platform.RARIBLE
        )
        depositInitialBalance(userSender1.from(), makeValue.value.plus(BigInteger.TWO))
        `test insert order`(version)
        assertEquals(BigInteger.TWO, getEthBalance(userSender1.from()))
    }

    private suspend fun `test insert order`(version: OrderVersion) {
        val upsertTimestamp = exchange.upsertOrder(version.toOrderExactFields().forTx())
            .withSender(userSender1)
            .let {
                if (version.make.type is EthAssetType) {
                    it.withValue(version.make.value.value)
                } else {
                    it
                }
            }
            .execute()
            .verifySuccess()
            .getTimestamp()

        Wait.waitAssert {
            // Log descriptor creates a new instance of OrderVersion with a different 'id'
            fun OrderVersion.ignoreVersionId() = copy(id = ObjectId(0, 0))

            val items = exchangeHistoryRepository.findByItemType(ItemType.ON_CHAIN_ORDER).collectList().awaitFirst()
            Assertions.assertThat(items).hasSize(1)
            val onChainVersion = (items.single().data as OnChainOrder).order.ignoreVersionId()
            assertEquals(version.ignoreVersionId().copy(createdAt = upsertTimestamp), onChainVersion)
        }

        Wait.waitAssert {
            val savedOrder = orderRepository.findById(version.hash)
            assertNotNull(savedOrder)

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

            val expectedOrder = version.toOrderExactFields().copy(
                createdAt = upsertTimestamp,
                lastUpdateAt = upsertTimestamp,
                priceHistory = listOf(
                    OrderPriceHistoryRecord(
                        date = upsertTimestamp,
                        makeValue = version.make.value.value.toBigDecimal(decimals(version.make.type)),
                        takeValue = version.take.value.value.toBigDecimal(decimals(version.take.type))
                    )
                )
            )
            assertEquals(expectedOrder, savedOrder)
        }
    }
}
