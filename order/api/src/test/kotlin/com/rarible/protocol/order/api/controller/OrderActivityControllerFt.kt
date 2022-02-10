package com.rarible.protocol.order.api.controller

import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.createNftOwnershipDto
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderVersion
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

@IntegrationTest
class OrderActivityControllerFt : AbstractIntegrationTest() {
    internal companion object {
        private val now = Instant.ofEpochSecond(Instant.now().epochSecond)

        @JvmStatic
        fun activityVersionFilterData() = Stream.of(
            Arguments.of(
                listOf(
                    createErc721BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.minus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.minus(2, ChronoUnit.MINUTES))
                ),
                listOf(
                    createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                ),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.BID)),
                ActivitySortDto.LATEST_FIRST
            ),
            run {
                val item1 = createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES))
                val item2 = createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES))
                val item3 = createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES))
                val item4 = createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES))
                Arguments.of(
                    listOf(
                        item4, item3, item2, item1,
                        createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                        createErc1155BidOrderVersion().copy(createdAt = now.plus(0, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            Arguments.of(
                listOf(
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(3, ChronoUnit.MINUTES))
                ),
                listOf(
                    createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                ),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.BID)),
                ActivitySortDto.EARLIEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createErc1155BidOrderVersion().copy(createdAt = now.minus(2, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.minus(1, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc721BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES))
                ),
                listOf(
                    createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                ),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.BID)),
                ActivitySortDto.EARLIEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createErc721ListOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.minus(1, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.minus(2, ChronoUnit.MINUTES))
                ),
                listOf(
                    createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion()
                ),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.LIST)),
                ActivitySortDto.LATEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createErc721BidOrderVersion().copy(createdAt = now.plus(8, ChronoUnit.MINUTES)),
                    createErc721ListOrderVersion().copy(createdAt = now.plus(7, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(6, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.plus(5, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(4, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.plus(3, ChronoUnit.MINUTES)),
                    createErc1155BidOrderVersion().copy(createdAt = now.plus(2, ChronoUnit.MINUTES)),
                    createErc1155ListOrderVersion().copy(createdAt = now.plus(1, ChronoUnit.MINUTES))
                ),
                emptyList<OrderVersion>(),
                OrderActivityFilterAllDto(
                    listOf(
                        OrderActivityFilterAllDto.Types.BID,
                        OrderActivityFilterAllDto.Types.LIST
                    )
                ),
                ActivitySortDto.LATEST_FIRST
            ),
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(10, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(9, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(8, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(7, ChronoUnit.MINUTES)
                        )
                    ),
                    listOf(
                        createErc721BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(12, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(11, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(6, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(5, ChronoUnit.MINUTES)
                        )
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.MAKE_BID),
                        from = (now.plus(7, ChronoUnit.MINUTES)).epochSecond,
                        to = (now.plus(10, ChronoUnit.MINUTES)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion().copy(maker = maker, createdAt = now.plus(2, ChronoUnit.MINUTES)),
                        createErc1155BidOrderVersion().copy(maker = maker, createdAt = now.plus(1, ChronoUnit.MINUTES)),
                        createErc1155BidOrderVersion().copy(
                            maker = maker,
                            createdAt = now.minus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(maker = maker, createdAt = now.minus(2, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.MAKE_BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val taker1 = AddressFactory.create()
                val taker2 = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion().copy(taker = taker1, createdAt = now + Duration.ofMinutes(2)),
                        createErc1155BidOrderVersion().copy(taker = taker2, createdAt = now + Duration.ofMinutes(1)),
                        createErc1155BidOrderVersion().copy(taker = taker1, createdAt = now - Duration.ofMinutes(1)),
                        createErc1155BidOrderVersion().copy(taker = taker2, createdAt = now - Duration.ofMinutes(2))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(taker1, taker2),
                        listOf(OrderActivityFilterByUserDto.Types.GET_BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion().copy(maker = maker, createdAt = now.plus(2, ChronoUnit.MINUTES)),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.minus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.minus(2, ChronoUnit.MINUTES)
                        )
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.LIST)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(10, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(11, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(12, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(13, ChronoUnit.MINUTES)
                        )
                    ),
                    listOf(
                        createErc721ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(2, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(14, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(15, ChronoUnit.MINUTES)
                        )
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.LIST),
                        from = (now.plus(10, ChronoUnit.MINUTES)).epochSecond,
                        to = (now.plus(13, ChronoUnit.MINUTES)).epochSecond
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(13, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(12, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(11, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(10, ChronoUnit.MINUTES)
                        )
                    ),
                    listOf(
                        createErc721ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(2, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(14, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker,
                            createdAt = now.plus(15, ChronoUnit.MINUTES)
                        )
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.LIST),
                        from = (now.plus(10, ChronoUnit.MINUTES)).epochSecond,
                        to = (now.plus(13, ChronoUnit.MINUTES)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker1 = AddressFactory.create()
                val maker2 = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion().copy(maker = maker1, createdAt = now.plus(2, ChronoUnit.MINUTES)),
                        createErc1155ListOrderVersion().copy(
                            maker = maker1,
                            createdAt = now.plus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155BidOrderVersion().copy(
                            maker = maker2,
                            createdAt = now.minus(1, ChronoUnit.MINUTES)
                        ),
                        createErc1155ListOrderVersion().copy(
                            maker = maker2,
                            createdAt = now.minus(2, ChronoUnit.MINUTES)
                        )
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByUserDto(
                        listOf(maker1, maker2),
                        listOf(OrderActivityFilterByUserDto.Types.LIST, OrderActivityFilterByUserDto.Types.MAKE_BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of(BigInteger.valueOf((1L..1000L).random()))

                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion()
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now.plus(2, ChronoUnit.MINUTES)),
                        createErc721BidOrderVersion()
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now.plus(1, ChronoUnit.MINUTES)),
                        createErc721BidOrderVersion()
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now.minus(1, ChronoUnit.MINUTES)),
                        createErc1155BidOrderVersion()
                            .withTakeNft(token, tokenId)
                            .withCreatedAt(now.minus(2, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByItemDto(
                        token,
                        tokenId.value,
                        listOf(OrderActivityFilterByItemDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of(BigInteger.valueOf((1L..1000L).random()))

                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion()
                            .withMakeNft(token, tokenId)
                            .withCreatedAt(now.plus(2, ChronoUnit.MINUTES)),
                        createErc721ListOrderVersion()
                            .withMakeNft(token, tokenId)
                            .withCreatedAt(now.plus(1, ChronoUnit.MINUTES)),
                        createErc721ListOrderVersion()
                            .withMakeNft(token, tokenId)
                            .withCreatedAt(now.minus(1, ChronoUnit.MINUTES)),
                        createErc1155ListOrderVersion()
                            .withMakeNft(token, tokenId)
                            .withCreatedAt(now.minus(2, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByItemDto(
                        token,
                        tokenId.value,
                        listOf(OrderActivityFilterByItemDto.Types.LIST)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createErc721BidOrderVersion()
                            .withTakeToken(token)
                            .withCreatedAt(now.plus(2, ChronoUnit.MINUTES)),
                        createErc721BidOrderVersion()
                            .withTakeToken(token)
                            .withCreatedAt(now.plus(1, ChronoUnit.MINUTES)),
                        createErc721BidOrderVersion()
                            .withTakeToken(token)
                            .withCreatedAt(now.minus(1, ChronoUnit.MINUTES)),
                        createErc1155BidOrderVersion()
                            .withTakeToken(token)
                            .withCreatedAt(now.minus(2, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByCollectionDto(
                        token,
                        listOf(OrderActivityFilterByCollectionDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.plus(2, ChronoUnit.MINUTES)),
                        createErc721ListOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.plus(1, ChronoUnit.MINUTES)),
                        createErc721ListOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.minus(1, ChronoUnit.MINUTES)),
                        createErc1155ListOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.minus(2, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion(),
                        createErc721ListOrderVersion(), createErc1155ListOrderVersion(), createErc721ListOrderVersion()
                    ),
                    OrderActivityFilterByCollectionDto(
                        token,
                        listOf(OrderActivityFilterByCollectionDto.Types.LIST)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createErc721ListOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.plus(2, ChronoUnit.MINUTES)),
                        createCollectionOrderVersion()
                            .withMakeToken(token)
                            .withCreatedAt(now.plus(1, ChronoUnit.MINUTES))
                    ),
                    listOf(
                        createErc721BidOrderVersion(), createErc1155BidOrderVersion(), createErc721BidOrderVersion()
                    ),
                    OrderActivityFilterByCollectionDto(
                        token,
                        listOf(OrderActivityFilterByCollectionDto.Types.LIST)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of(BigInteger.valueOf((1L..1000L).random()))

                Arguments.of(
                    listOf(createCollectionBidOrderVersion().withTakeToken(token)),
                    listOf(createErc721ListOrderVersion()),
                    OrderActivityFilterByItemDto(
                        token,
                        tokenId.value,
                        listOf(OrderActivityFilterByItemDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }
        )

        @JvmStatic
        fun activityHistoryFilterData() = Stream.of(
            Arguments.of(
                listOf(
                    createLogEvent(orderErc721SellSideMatch().copy(date = now.plus(2, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721SellSideMatch().copy(date = now.plus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellSideMatch().copy(date = now.minus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellSideMatch().copy(date = now.minus(2, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellSideMatch().copy(date = now.minus(3, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellSideMatch().copy(date = now.minus(4, ChronoUnit.MINUTES)))
                ),
                listOf(
                    createLogEvent(orderErc1155BidCancel().copy(date = now.minus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721BidCancel().copy(date = now.minus(4, ChronoUnit.MINUTES)))
                ),
                listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.MATCH)),
                ActivitySortDto.LATEST_FIRST
            ),
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(1, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(0, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.minus(1, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.minus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.minus(3, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155BidCancel().copy(maker = maker, date = now.minus(2, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(listOf(maker), listOf(OrderActivityFilterByUserDto.Types.SELL)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(10, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(9, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(8, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(7, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(6, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(5, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(12, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(11, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellCancel().copy(
                                maker = maker,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.SELL),
                        from = (now.plus(5, ChronoUnit.MINUTES)).epochSecond,
                        to = (now.plus(10, ChronoUnit.MINUTES)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(5, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(6, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(7, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(8, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(9, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(10, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(12, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(11, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellCancel().copy(
                                maker = maker,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(
                        listOf(maker),
                        listOf(OrderActivityFilterByUserDto.Types.SELL),
                        from = (now.plus(5, ChronoUnit.MINUTES)).epochSecond,
                        to = (now.plus(10, ChronoUnit.MINUTES)).epochSecond
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },
            run {
                val maker1 = AddressFactory.create()
                val maker2 = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker1,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch().copy(
                                maker = maker2,
                                date = now.plus(1, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker1,
                                date = now.plus(0, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker1,
                                date = now.minus(1, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker2,
                                date = now.minus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createLogEvent(
                            orderErc1155SellSideMatch().copy(
                                maker = maker2,
                                date = now.minus(3, ChronoUnit.MINUTES)
                            )
                        )
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker1, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155BidCancel().copy(maker = maker2, date = now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(
                        listOf(maker1, maker2),
                        listOf(OrderActivityFilterByUserDto.Types.SELL)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.plus(2, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.plus(1, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.plus(0, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.minus(1, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.minus(2, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.minus(3, ChronoUnit.MINUTES))
                        )
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByItemDto(token, tokenId.value, listOf(OrderActivityFilterByItemDto.Types.MATCH)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.plus(2, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeNft(token, tokenId)
                                .withDate(now.plus(1, ChronoUnit.MINUTES))
                        )
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellCancel()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(2, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByItemDto(token, tokenId.value, listOf(OrderActivityFilterByItemDto.Types.MATCH)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.plus(2, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.plus(1, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.plus(0, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.minus(1, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.minus(2, ChronoUnit.MINUTES))
                        ),
                        createLogEvent(
                            orderErc721SellSideMatch()
                                .withMakeToken(token)
                                .withDate(now.minus(3, ChronoUnit.MINUTES))
                        )
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByCollectionDto(token, listOf(OrderActivityFilterByCollectionDto.Types.MATCH)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            //Data for cancel_sell and cancel_bid
            Arguments.of(
                listOf(
                    createLogEvent(orderErc1155SellCancel().copy(date = now.minus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellCancel().copy(date = now.minus(2, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721SellCancel().copy(date = now.minus(3, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721SellCancel().copy(date = now.minus(4, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721SellCancel().copy(date = now.minus(5, ChronoUnit.MINUTES)))
                ),
                listOf(
                    createLogEvent(orderErc721SellSideMatch().copy(date = now.plus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155BidCancel().copy(date = now.minus(1, ChronoUnit.MINUTES)))
                ),
                listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.CANCEL_LIST)),
                ActivitySortDto.LATEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createLogEvent(orderErc1155BidCancel().copy(date = now.minus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155BidCancel().copy(date = now.minus(2, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721BidCancel().copy(date = now.minus(3, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721BidCancel().copy(date = now.minus(4, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc721BidCancel().copy(date = now.minus(5, ChronoUnit.MINUTES)))
                ),
                listOf(
                    createLogEvent(orderErc721SellSideMatch().copy(date = now.plus(1, ChronoUnit.MINUTES))),
                    createLogEvent(orderErc1155SellCancel().copy(date = now.minus(1, ChronoUnit.MINUTES)))
                ),
                listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                OrderActivityFilterAllDto(listOf(OrderActivityFilterAllDto.Types.CANCEL_BID)),
                ActivitySortDto.LATEST_FIRST
            ),
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155BidCancel().copy(maker = maker, date = now.minus(2, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(listOf(maker), listOf(OrderActivityFilterByUserDto.Types.CANCEL_LIST)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val maker = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().copy(maker = maker, date = now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721SellCancel().copy(maker = maker, date = now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155SellCancel().copy(maker = maker, date = now.minus(2, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByUserDto(listOf(maker), listOf(OrderActivityFilterByUserDto.Types.CANCEL_BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByItemDto(token, tokenId.value, listOf(OrderActivityFilterByItemDto.Types.CANCEL_LIST)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(7, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(8, ChronoUnit.MINUTES)))
                    ),
                    emptyList<LogEvent>(),
                    emptyList<OrderVersion>(),
                    OrderActivityFilterByCollectionDto(token, listOf(OrderActivityFilterByCollectionDto.Types.MATCH)),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeNft(token, tokenId).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc1155SellSideMatch()),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeNft(token, tokenId).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByItemDto(token, tokenId.value, listOf(OrderActivityFilterByItemDto.Types.CANCEL_BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155SellSideMatch().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByCollectionDto(token, listOf(OrderActivityFilterByCollectionDto.Types.CANCEL_LIST)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.minus(1, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.minus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721BidCancel().withTakeToken(token).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(
                        createLogEvent(orderErc1155SellSideMatch().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc1155SellSideMatch().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.plus(0, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellCancel().withMakeToken(token).withDate(now.minus(3, ChronoUnit.MINUTES)))
                    ),
                    listOf(createErc721BidOrderVersion(), createErc1155ListOrderVersion()),
                    OrderActivityFilterByCollectionDto(token, listOf(OrderActivityFilterByCollectionDto.Types.CANCEL_BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val items1 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))
                val items2 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))
                val items3 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))
                val items4 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))
                val items5 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))
                val items6 = createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(9, ChronoUnit.MINUTES)))

                Arguments.of(
                    listOf(
                        items6, items5, items4, items3, items2, items1,
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(2, ChronoUnit.MINUTES))),
                        createLogEvent(orderErc721SellSideMatch().withMakeToken(token).withDate(now.plus(1, ChronoUnit.MINUTES)))
                    ),
                    emptyList<LogEvent>(),
                    emptyList<OrderVersion>(),
                    OrderActivityFilterByCollectionDto(token, listOf(OrderActivityFilterByCollectionDto.Types.MATCH)),
                    ActivitySortDto.LATEST_FIRST
                )
            }
        )
    }

    @ParameterizedTest
    @MethodSource("activityVersionFilterData")
    fun `should find version activity by pagination`(
        orderVersions: List<OrderVersion>,
        otherTypes: List<OrderVersion>,
        filter: OrderActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {
        saveVersion(*orderVersions.shuffled().toTypedArray())
        saveVersion(*otherTypes.shuffled().toTypedArray())
        prepareNftClient(filter, orderVersions)

        val allActivities = mutableListOf<OrderActivityDto>()

        var continuation: String? = null
        do {
            val activities = orderActivityClient.getOrderActivities(filter, continuation, 2, sort).awaitFirst()
            assertThat(activities.items).hasSizeLessThanOrEqualTo(2)

            allActivities.addAll(activities.items)
            continuation = activities.continuation
        } while (continuation != null)

        assertThat(allActivities).hasSize(orderVersions.size)

        allActivities.forEachIndexed { index, orderActivityDto ->
            checkOrderActivityDto(orderActivityDto, orderVersions[index])
        }
    }

    @ParameterizedTest
    @MethodSource("activityVersionFilterData")
    fun `should find all version activity`(
        orderVersions: List<OrderVersion>,
        otherTypes: List<OrderVersion>,
        filter: OrderActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {
        saveVersion(*orderVersions.shuffled().toTypedArray())
        saveVersion(*otherTypes.shuffled().toTypedArray())
        prepareNftClient(filter, orderVersions)

        Wait.waitAssert {
            val activities = orderActivityClient.getOrderActivities(filter, null, null, sort).awaitFirst()

            assertThat(activities.items).hasSize(orderVersions.size)

            activities.items.forEachIndexed { index, orderActivityDto ->
                checkOrderActivityDto(orderActivityDto, orderVersions[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("activityHistoryFilterData")
    fun `should find history activity by pagination`(
        logs: List<LogEvent>,
        otherLogs: List<LogEvent>,
        otherVersions: List<OrderVersion>,
        filter: OrderActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {
        saveHistory(*logs.shuffled().toTypedArray())
        saveHistory(*otherLogs.shuffled().toTypedArray())
        saveVersion(*otherVersions.shuffled().toTypedArray())

        val allActivities = mutableListOf<OrderActivityDto>()

        var continuation: String? = null
        do {
            val activities = orderActivityClient.getOrderActivities(filter, continuation, 2, sort).awaitFirst()
            assertThat(activities.items).hasSizeLessThanOrEqualTo(2)

            allActivities.addAll(activities.items)
            continuation = activities.continuation
        } while (continuation != null)

        assertThat(allActivities).hasSize(logs.size)

        allActivities.forEachIndexed { index, orderActivityDto ->
            checkOrderActivityDto(orderActivityDto, logs[index])
        }
    }

    @ParameterizedTest
    @MethodSource("activityHistoryFilterData")
    fun `should find all history activity`(
        logs: List<LogEvent>,
        otherLogs: List<LogEvent>,
        otherVersions: List<OrderVersion>,
        filter: OrderActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {
        saveHistory(*logs.shuffled().toTypedArray())
        saveHistory(*otherLogs.shuffled().toTypedArray())
        saveVersion(*otherVersions.shuffled().toTypedArray())

        Wait.waitAssert {
            val activities = orderActivityClient.getOrderActivities(filter, null, null, sort).awaitFirst()

            assertThat(activities.items).hasSize(logs.size)

            activities.items.forEachIndexed { index, orderActivityDto ->
                checkOrderActivityDto(orderActivityDto, logs[index])
            }
        }
    }

    @Test
    fun `should get zero price if make nft value is zero`() = runBlocking {
        val version = createErc721ListOrderVersion().withMakeValue(EthUInt256.ZERO)
        saveVersion(version)

        val filter =
            OrderActivityFilterByUserDto(listOf(version.maker), listOf(OrderActivityFilterByUserDto.Types.LIST))
        val activities = orderActivityClient.getOrderActivities(filter, null, null, null).awaitFirst()
        checkOrderActivityDto(activities.items.single(), version)
        assertThat((activities.items.single() as? OrderActivityListDto)?.price).isEqualTo(BigDecimal.ZERO)
        checkOrderActivityDto(activities.items.single(), version)
    }

    fun prepareNftClient(filter: OrderActivityFilterDto, orderVersions: List<OrderVersion>) {
        if (filter is OrderActivityFilterByUserDto &&
            filter.types.contains(OrderActivityFilterByUserDto.Types.GET_BID)
        ) {
            filter.users.forEach { user ->
                val erc721Tokens = orderVersions
                    .filter { it.taker == user }
                    .map { it.take.type }
                    .filterIsInstance<Erc721AssetType>()
                    .map { nftAsset ->
                           createNftOwnershipDto().copy(
                                contract = nftAsset.token,
                                tokenId = nftAsset.tokenId.value
                           )
                    }

                val erc1155Tokens = orderVersions
                    .filter { it.taker == user }
                    .map { it.take.type }
                    .filterIsInstance<Erc1155AssetType>()
                    .map { nftAsset ->
                           createNftOwnershipDto().copy(
                               contract = nftAsset.token,
                               tokenId = nftAsset.tokenId.value
                        )
                    }

                coEvery { nftOwnership.getNftOwnershipsByOwner(eq(user.prefixed()), any(), any()) } returns Mono.just(
                    NftOwnershipsDto(
                        total = (erc721Tokens.size + erc1155Tokens.size).toLong(),
                        continuation = null,
                        ownerships = erc721Tokens + erc1155Tokens
                    )
                )
            }
        }
    }

    private fun checkOrderActivityDto(orderActivityDto: OrderActivityDto, version: OrderVersion) {
        assertThat(orderActivityDto.id).isEqualTo(version.id.toString())
        assertThat(orderActivityDto.date).isEqualTo(version.createdAt)
    }

    private fun checkOrderActivityDto(orderActivityDto: OrderActivityDto, history: LogEvent) {
        assertThat(orderActivityDto.id).isEqualTo(history.id.toString())
        assertThat(orderActivityDto.date).isEqualTo((history.data as OrderExchangeHistory).date)
    }

    private suspend fun saveHistory(vararg history: LogEvent) {
        history.forEach { exchangeHistoryRepository.save(it).awaitFirst() }
    }

    private suspend fun saveVersion(vararg version: OrderVersion) {
        version.forEach { orderVersionRepository.save(it).awaitFirst() }
    }
}
