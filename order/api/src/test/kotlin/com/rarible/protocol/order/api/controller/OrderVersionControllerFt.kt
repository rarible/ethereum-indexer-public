package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderBidDto
import com.rarible.protocol.dto.OrderBidStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.converters.dto.BidStatusDtoConverter
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.BidStatus
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.stream.Stream

@IntegrationTest
class OrderVersionControllerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderVersionRepository: OrderVersionRepository

    @BeforeEach
    override fun setupDatabase() = runBlocking {
        super.setupDatabase()
        orderVersionRepository.createIndexes()
    }

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
        saveVersion(*orderVersionBids.map { it.orderVersion }.shuffled().toTypedArray())
        saveVersion(*otherVersions.shuffled().toTypedArray())

        orderVersionBids.forEach { saveOrder(createOrder().copy(hash = it.orderVersion.hash), it.status) }
        otherVersions.forEach { saveOrder(createOrder().copy(hash = it.hash)) }

        val versions = orderBidsClient.getBidsByItem(
            params.token.hex(),
            params.tokenId.value.toString(),
            params.status,
            params.maker?.hex(),
            params.platform,
            params.startDate?.atOffset(ZoneOffset.UTC),
            params.endDate?.atOffset(ZoneOffset.UTC),
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
                3
            ).awaitFirst()

            assertThat(versions.items).hasSize(3)
        }

        private suspend fun prepareDatabase(token: Address, tokenId: EthUInt256) {
            listOf(
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
            ).forEach { orderVersionBid ->
                saveVersion(orderVersionBid.orderVersion)
                saveOrder(createOrder().copy(hash = orderVersionBid.orderVersion.hash), orderVersionBid.status)
            }
        }
    }

    private fun checkOrderActivityDto(orderVersionDto: OrderBidDto, versionBid: OrderVersionBid) {
        assertThat(orderVersionDto.status).isEqualTo(BidStatusDtoConverter.convert(versionBid.status))
        assertThat(orderVersionDto.orderHash).isEqualTo(versionBid.orderVersion.hash)
        assertThat(orderVersionDto.createdAt).isEqualTo(versionBid.orderVersion.createdAt)
    }

    private suspend fun saveVersion(vararg version: OrderVersion) {
        version.forEach { orderVersionRepository.save(it).awaitFirst() }
    }

    private suspend fun saveOrder(order: Order, status: BidStatus = BidStatus.HISTORICAL) {
        val updatedOrder = when (status) {
            BidStatus.CANCELLED -> order.copy(cancelled = true)
            BidStatus.INACTIVE -> order.copy(makeStock = EthUInt256.ZERO)
            else -> order.copy(cancelled = false, makeStock = EthUInt256.TEN)
        }
        orderRepository.save(updatedOrder)
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
        val maker: Address? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null
    )
}
