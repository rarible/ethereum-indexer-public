package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.order.api.data.createErc1155BidOrderVersion
import com.rarible.protocol.order.api.data.createErc1155ListOrderVersion
import com.rarible.protocol.order.api.data.createErc721BidOrderVersion
import com.rarible.protocol.order.api.data.createErc721ListOrderVersion
import com.rarible.protocol.order.api.data.createLogEvent
import com.rarible.protocol.order.api.data.withCreatedAt
import com.rarible.protocol.order.api.data.withOrigin
import com.rarible.protocol.order.api.data.withTakeNft
import com.rarible.protocol.order.api.data.withTakePriceUsd
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.converters.dto.BidStatusConverter
import com.rarible.protocol.order.core.model.BidStatus
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.toOnChainOrder
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@IntegrationTest
class OrderVersionControllerFt : AbstractIntegrationTest() {

    internal companion object {
        private val now: Instant = nowMillis()

        @JvmStatic
        fun versionFilterData() = Stream.of(
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        OrderVersionBid(
                            createErc721BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(4))
                                .withTakeNft(token, tokenId)
                                .withCreatedAt(now + Duration.ofMinutes(1)),
                            BidStatus.ACTIVE
                        ),
                        OrderVersionBid(
                            createErc1155BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(3))
                                .withTakeNft(token, tokenId)
                                .withCreatedAt(now + Duration.ofMinutes(2)),
                            BidStatus.ACTIVE
                        ),
                        OrderVersionBid(
                            createErc1155BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(2))
                                .withTakeNft(token, tokenId)
                                .withCreatedAt(now + Duration.ofMinutes(3)),
                            BidStatus.INACTIVE
                        ),
                        OrderVersionBid(
                            createErc1155BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(1))
                                .withTakeNft(token, tokenId)
                                .withCreatedAt(now + Duration.ofMinutes(4)),
                            BidStatus.CANCELLED
                        )
                    ),
                    listOf(
                        createErc721ListOrderVersion(),
                        createErc1155ListOrderVersion(),
                        createErc721BidOrderVersion(),
                        createErc721BidOrderVersion()
                    ),
                    BidByItemParams(token, tokenId)
                )
            },
            run {
                val token = randomAddress()
                val tokenId = EthUInt256.of((1L..1000L).random())
                val origin = randomAddress()

                Arguments.of(
                    listOf(
                        OrderVersionBid(
                            createErc721BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(4))
                                .withTakeNft(token, tokenId)
                                .withOrigin(origin)
                                .withCreatedAt(now + Duration.ofMinutes(1)),
                            BidStatus.ACTIVE
                        ),
                        OrderVersionBid(
                            createErc1155BidOrderVersion()
                                .withTakePriceUsd(BigDecimal.valueOf(3))
                                .withOrigin(origin)
                                .withTakeNft(token, tokenId)
                                .withCreatedAt(now + Duration.ofMinutes(2)),
                            BidStatus.ACTIVE
                        )
                    ),
                    listOf(
                        createErc1155BidOrderVersion()
                            .withTakePriceUsd(BigDecimal.valueOf(2))
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now + Duration.ofMinutes(3)),
                        createErc1155BidOrderVersion()
                            .withTakePriceUsd(BigDecimal.valueOf(1))
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now + Duration.ofMinutes(4))
                    ),
                    BidByItemParams(token, tokenId, origin = origin)
                )
            }
        )

    }

    @ParameterizedTest
    @MethodSource("versionFilterData")
    fun `should find all bids`(
        orderVersionBids: List<OrderVersionBid>,
        otherVersions: List<OrderVersion>,
        params: BidByItemParams
    ) = runBlocking {
        saveOrders(orderVersionBids + otherVersions.map { OrderVersionBid(it, BidStatus.ACTIVE) })

        Wait.waitAssert {
            val versions = orderClient.getOrderBidsByItemAndByStatus(
                params.token.hex(),
                params.tokenId.value.toString(),
                params.maker?.let { listOf(it) },
                params.origin?.hex(),
                params.platform,
                null,
                null,
                params.status,
                null,
                params.startDate?.epochSecond,
                params.endDate?.epochSecond
            ).awaitFirst()

            assertThat(versions.orders).hasSize(orderVersionBids.size)

            versions.orders.forEachIndexed { index, orderVersionDto ->
                val versionBid = orderVersionBids[index]
                assertThat(orderVersionDto.status).isEqualTo(BidStatusConverter.convert(versionBid.status))
                assertThat(orderVersionDto.hash).isEqualTo(versionBid.orderVersion.hash)
                assertThat(orderVersionDto.createdAt).isEqualTo(versionBid.orderVersion.createdAt)
            }
        }
    }

    @Test
    internal fun `should find on-chain bid`() = runBlocking<Unit> {
        val bidVersion = createErc721BidOrderVersion().toOnChainOrder()
        val logEvent = createLogEvent(bidVersion)
        exchangeHistoryRepository.save(logEvent).awaitFirst()
        orderReduceService.updateOrder(bidVersion.hash)
        val paginationDto = orderClient.getOrderBidsByItemAndByStatus(
            (bidVersion.take.type as Erc721AssetType).token.hex(),
            (bidVersion.take.type as Erc721AssetType).tokenId.value.toString(),
            null,
            null,
            PlatformDto.RARIBLE,
            null,
            null,
            OrderStatusDto.values().toList(),
            null,
            null,
            null
        ).awaitFirst()
        assertThat(paginationDto.orders).hasSize(1)
        val bidDto = paginationDto.orders.single()
        assertEquals(bidVersion.hash, bidDto.hash)
        assertEquals(OrderStatusDto.INACTIVE, bidDto.status)
        assertTrue(bidDto is RaribleV2OrderDto)
    }

    @Nested
    inner class OrderVersionFetchWithCorrectLimitFt {
        @Test
        fun `should fetch correct size of active bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderClient.getOrderBidsByItemAndByStatus(
                token.hex(),
                tokenId.value.toString(),
                null,
                null,
                null,
                null,
                3,
                listOf(OrderStatusDto.ACTIVE),
                null,
                null,
                null
            ).awaitFirst()

            assertThat(versions.orders).hasSize(3)
        }

        @Test
        fun `should fetch correct size of canceled bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderClient.getOrderBidsByItemAndByStatus(
                token.hex(),
                tokenId.value.toString(),
                null,
                null,
                null,
                null,
                2,
                listOf(OrderStatusDto.CANCELLED),
                null,
                null,
                null
            ).awaitFirst()

            assertThat(versions.orders).hasSize(2)
        }

        @Test
        fun `should fetch correct size of inactive bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderClient.getOrderBidsByItemAndByStatus(
                token.hex(),
                tokenId.value.toString(),
                null,
                null,
                null,
                null,
                3,
                listOf(OrderStatusDto.INACTIVE),
                null,
                null,
                null
            ).awaitFirst()

            assertThat(versions.orders).hasSize(3)
        }

        private suspend fun prepareDatabase(token: Address, tokenId: EthUInt256) {
            val orderVersionBids = listOf(
                OrderVersionBid(
                    createErc721BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(20))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(1)),
                    BidStatus.ACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(19))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(2)),
                    BidStatus.ACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(18))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(3)),
                    BidStatus.INACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(17))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(4)),
                    BidStatus.CANCELLED
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(16))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(3)),
                    BidStatus.ACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(15))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(2)),
                    BidStatus.INACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(14))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(1)),
                    BidStatus.INACTIVE
                ),
                OrderVersionBid(
                    createErc1155BidOrderVersion()
                        .withTakePriceUsd(BigDecimal.valueOf(13))
                        .withTakeNft(token, tokenId)
                        .withCreatedAt(now + Duration.ofMinutes(1)),
                    BidStatus.CANCELLED
                )
            )
            saveOrders(orderVersionBids)
        }
    }

    private fun checkOrderActivityDto(orderVersionDto: OrderDto, versionBid: OrderVersionBid) {
        assertThat(orderVersionDto.status).isEqualTo(BidStatusConverter.convert(versionBid.status))
        assertThat(orderVersionDto.hash).isEqualTo(versionBid.orderVersion.hash)
        assertThat(orderVersionDto.createdAt).isEqualTo(versionBid.orderVersion.createdAt)
    }

    private suspend fun saveOrders(version: List<OrderVersionBid>) {
        clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } answers {
            val order = arg<Order>(0)
            val totalOrigin = (order.data as? OrderRaribleV2DataV1)?.originFees
                .orEmpty().sumBy { it.value.value.toInt() }
            val balance = EthUInt256.of(1000 * (1 + totalOrigin.toDouble() / 10000).toInt())
            when (version.firstOrNull { it.orderVersion.maker == order.maker }?.status) {
                BidStatus.ACTIVE, BidStatus.HISTORICAL, BidStatus.FILLED -> MakeBalanceState(balance)
                else -> MakeBalanceState(EthUInt256.ZERO)
            }
        }
        for ((orderVersion, status) in version) {
            if (status == BidStatus.CANCELLED) {
                cancelOrder(orderVersion.hash)
            }
            save(orderVersion)
        }
    }

    data class OrderVersionBid(
        val orderVersion: OrderVersion,
        val status: BidStatus
    )

    data class BidByItemParams(
        val token: Address,
        val tokenId: EthUInt256,
        val status: List<OrderStatusDto> = OrderStatusDto.values().toList(),
        val platform: PlatformDto = PlatformDto.RARIBLE,
        val origin: Address? = null,
        val maker: Address? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null
    )
}
