package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderBidDto
import com.rarible.protocol.dto.OrderBidStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.data.createErc1155BidOrderVersion
import com.rarible.protocol.order.api.data.createErc1155ListOrderVersion
import com.rarible.protocol.order.api.data.createErc721BidOrderVersion
import com.rarible.protocol.order.api.data.createErc721ListOrderVersion
import com.rarible.protocol.order.api.data.withCreatedAt
import com.rarible.protocol.order.api.data.withOrigin
import com.rarible.protocol.order.api.data.withTakeNft
import com.rarible.protocol.order.api.data.withTakePriceUsd
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.converters.dto.BidStatusDtoConverter
import com.rarible.protocol.order.core.model.BidStatus
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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

@Disabled
@IntegrationTest
class OrderVersionControllerDepricatedFt : AbstractIntegrationTest() {

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
                    BidByItemParams(token, tokenId, startDate = Instant.now())
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
                    BidByItemParams(token, tokenId, origin = origin, startDate = Instant.now())
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

        val versions = orderBidsClient.getBidsByItem(
            params.token.hex(),
            params.tokenId.value.toString(),
            params.status,
            params.maker?.hex(),
            params.origin?.hex(),
            params.platform,
            params.startDate,
            params.endDate,
            null,
            null
        ).awaitFirst()

        assertThat(versions.items).hasSize(orderVersionBids.size)

        versions.items.forEachIndexed { index, orderVersionDto ->
            checkOrderActivityDto(orderVersionDto, orderVersionBids[index])
        }
    }

    @Nested
    inner class OrderVersionFetchWithCorrectLimitFt {
        @Test
        fun `should fetch correct size of active bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderBidsClient.getBidsByItem(
                token.hex(),
                tokenId.value.toString(),
                listOf(OrderBidStatusDto.ACTIVE),
                null,
                null,
                null,
                null,
                null,
                null,
                3
            ).awaitFirst()

            assertThat(versions.items).hasSize(3)
        }

        @Test
        fun `should fetch correct size of canceled bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderBidsClient.getBidsByItem(
                token.hex(),
                tokenId.value.toString(),
                listOf(OrderBidStatusDto.CANCELLED),
                null,
                null,
                null,
                null,
                null,
                null,
                2
            ).awaitFirst()

            assertThat(versions.items).hasSize(2)
        }

        @Test
        fun `should fetch correct size of inactive bids`() = runBlocking<Unit> {
            val token = AddressFactory.create()
            val tokenId = EthUInt256.of((1L..1000L).random())
            prepareDatabase(token, tokenId)

            val versions = orderBidsClient.getBidsByItem(
                token.hex(),
                tokenId.value.toString(),
                listOf(OrderBidStatusDto.INACTIVE),
                null,
                null,
                null,
                null,
                null,
                null,
                3
            ).awaitFirst()

            assertThat(versions.items).hasSize(3)
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

    private fun checkOrderActivityDto(orderVersionDto: OrderBidDto, versionBid: OrderVersionBid) {
        assertThat(orderVersionDto.status).isEqualTo(BidStatusDtoConverter.convert(versionBid.status))
        assertThat(orderVersionDto.orderHash).isEqualTo(versionBid.orderVersion.hash)
        assertThat(orderVersionDto.createdAt).isEqualTo(versionBid.orderVersion.createdAt)
    }

    private suspend fun saveOrders(version: List<OrderVersionBid>) {
        io.mockk.clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } answers {
            val order = arg<Order>(0)
            when (version.firstOrNull { it.orderVersion.maker == order.maker }?.status) {
                BidStatus.ACTIVE, BidStatus.HISTORICAL, BidStatus.FILLED -> MakeBalanceState(EthUInt256.of(1000))
                else -> MakeBalanceState(EthUInt256.ZERO)
            }
        }
        for ((orderVersion, status) in version) {
            if (status == BidStatus.CANCELLED) {
                cancelOrder(orderVersion.hash)
            }
            orderUpdateService.save(orderVersion)
        }
    }

    data class OrderVersionBid(
        val orderVersion: OrderVersion,
        val status: BidStatus
    )

    data class BidByItemParams(
        val token: Address,
        val tokenId: EthUInt256,
        val status: List<OrderBidStatusDto> = OrderBidStatusDto.values().toList(),
        val platform: PlatformDto = PlatformDto.RARIBLE,
        val origin: Address? = null,
        val maker: Address? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null
    )
}
