package com.rarible.protocol.order.api.controller

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFilterDto
import com.rarible.protocol.order.api.data.createAuctionLogEvent
import com.rarible.protocol.order.api.data.randomAuction
import com.rarible.protocol.order.api.data.randomAuctionCreated
import com.rarible.protocol.order.api.data.randomBidPlaced
import com.rarible.protocol.order.api.data.randomCanceled
import com.rarible.protocol.order.api.data.randomFinished
import com.rarible.protocol.order.api.data.randomLogList
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

@IntegrationTest
class AuctionActivityControllerFt : AbstractIntegrationTest() {
    internal companion object {

        private val now = Instant.ofEpochSecond(Instant.now().epochSecond)
        private val randomAuctions = listOf(randomAuction())

        @JvmStatic
        fun activityFilterData() = Stream.of(
            run { // all
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(randomAuctionCreated(auction.contract, auction.auctionId))),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.CREATED)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomCanceled(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.CANCEL)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomFinished(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.FINISHED)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // all filter with all statuses
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction),
                    listOf(
                        createAuctionLogEvent(randomAuctionCreated(auction.contract, auction.auctionId).copy(date = now.plus(4, ChronoUnit.MINUTES))),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                date = now.plus(
                                    3,
                                    ChronoUnit.MINUTES
                                )
                            )
                        ),
                        createAuctionLogEvent(randomCanceled(auction.contract, auction.auctionId).copy(date = now.plus(2, ChronoUnit.MINUTES))),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId))
                    ),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(
                        listOf(
                            AuctionActivityFilterAllDto.Types.CREATED,
                            AuctionActivityFilterAllDto.Types.BID,
                            AuctionActivityFilterAllDto.Types.CANCEL,
                            AuctionActivityFilterAllDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // all filter with all statuses, reversed
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction),
                    listOf(
                        createAuctionLogEvent(randomAuctionCreated(auction.contract, auction.auctionId).copy(date = now.plus(4, ChronoUnit.MINUTES))),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                date = now.plus(
                                    3,
                                    ChronoUnit.MINUTES
                                )
                            )
                        ),
                        createAuctionLogEvent(randomCanceled(auction.contract, auction.auctionId).copy(date = now.plus(2, ChronoUnit.MINUTES))),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId))
                    ).asReversed(),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(
                        listOf(
                            AuctionActivityFilterAllDto.Types.CREATED,
                            AuctionActivityFilterAllDto.Types.BID,
                            AuctionActivityFilterAllDto.Types.CANCEL,
                            AuctionActivityFilterAllDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },

            // user
            run {
                val auction = randomAuction()
                val data = randomAuctionCreated(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        data.seller,
                        listOf(AuctionActivityFilterByUserDto.Types.CREATED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        data.buyer!!,
                        listOf(AuctionActivityFilterByUserDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomCanceled(auction.contract, auction.auctionId).copy(seller = auction.seller)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        data.seller!!,
                        listOf(AuctionActivityFilterByUserDto.Types.CANCEL)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomFinished(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        data.seller,
                        listOf(AuctionActivityFilterByUserDto.Types.FINISHED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // user filter with all statuses
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                seller = auction.seller,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                buyer = auction.seller,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                seller = auction.seller,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(seller = auction.seller))
                    ),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        auction.seller,
                        listOf(
                            AuctionActivityFilterByUserDto.Types.CREATED,
                            AuctionActivityFilterByUserDto.Types.BID,
                            AuctionActivityFilterByUserDto.Types.CANCEL,
                            AuctionActivityFilterByUserDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // user filter with all statuses, reversed
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                seller = auction.seller,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                buyer = auction.seller,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                seller = auction.seller,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(seller = auction.seller))
                    ).asReversed(),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByUserDto(
                        auction.seller,
                        listOf(
                            AuctionActivityFilterByUserDto.Types.CREATED,
                            AuctionActivityFilterByUserDto.Types.BID,
                            AuctionActivityFilterByUserDto.Types.CANCEL,
                            AuctionActivityFilterByUserDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },

            // item
            run {
                val auction = randomAuction()
                val data = randomAuctionCreated(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        data.sell.type.token,
                        data.sell.type.tokenId!!.value,
                        listOf(AuctionActivityFilterByItemDto.Types.CREATED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.contract, auction.auctionId).copy(sell = auction.sell)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        auction.sell.type.token,
                        auction.sell.type.tokenId!!.value,
                        listOf(AuctionActivityFilterByItemDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomCanceled(auction.contract, auction.auctionId).copy(sell = auction.sell)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        auction.sell.type.token,
                        auction.sell.type.tokenId!!.value,
                        listOf(AuctionActivityFilterByItemDto.Types.CANCEL)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomFinished(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        data.sell.type.token,
                        data.sell.type.tokenId!!.value,
                        listOf(AuctionActivityFilterByItemDto.Types.FINISHED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // item filter with all statuses
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(sell = auction.sell))
                    ),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        auction.sell.type.token,
                        auction.sell.type.tokenId!!.value,
                        listOf(
                            AuctionActivityFilterByItemDto.Types.CREATED,
                            AuctionActivityFilterByItemDto.Types.BID,
                            AuctionActivityFilterByItemDto.Types.CANCEL,
                            AuctionActivityFilterByItemDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // item filter with all statuses, reversed
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(sell = auction.sell))
                    ).asReversed(),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByItemDto(
                        auction.sell.type.token,
                        auction.sell.type.tokenId!!.value,
                        listOf(
                            AuctionActivityFilterByItemDto.Types.CREATED,
                            AuctionActivityFilterByItemDto.Types.BID,
                            AuctionActivityFilterByItemDto.Types.CANCEL,
                            AuctionActivityFilterByItemDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },

            // collection
            run {
                val auction = randomAuction()
                val data = randomAuctionCreated(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        data.sell.type.token,
                        listOf(AuctionActivityFilterByCollectionDto.Types.CREATED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.contract, auction.auctionId).copy(sell = auction.sell)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        auction.sell.type.token,
                        listOf(AuctionActivityFilterByCollectionDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomCanceled(auction.contract, auction.auctionId).copy(sell = auction.sell)
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        auction.sell.type.token,
                        listOf(AuctionActivityFilterByCollectionDto.Types.CANCEL)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomFinished(auction.contract, auction.auctionId)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        data.sell.type.token,
                        listOf(AuctionActivityFilterByCollectionDto.Types.FINISHED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // collection filter with all statuses
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(sell = auction.sell))
                    ),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        auction.sell.type.token,
                        listOf(
                            AuctionActivityFilterByCollectionDto.Types.CREATED,
                            AuctionActivityFilterByCollectionDto.Types.BID,
                            AuctionActivityFilterByCollectionDto.Types.CANCEL,
                            AuctionActivityFilterByCollectionDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run { // collection filter with all statuses, reversed
                val auction = randomAuction()
                Arguments.of(
                    listOf(auction) + randomAuctions,
                    listOf(
                        createAuctionLogEvent(
                            randomAuctionCreated(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(4, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(3, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(
                            randomCanceled(auction.contract, auction.auctionId).copy(
                                sell = auction.sell,
                                date = now.plus(2, ChronoUnit.MINUTES)
                            )
                        ),
                        createAuctionLogEvent(randomFinished(auction.contract, auction.auctionId).copy(sell = auction.sell))
                    ).asReversed(),
                    randomLogList(randomAuctions),
                    AuctionActivityFilterByCollectionDto(
                        auction.sell.type.token,
                        listOf(
                            AuctionActivityFilterByCollectionDto.Types.CREATED,
                            AuctionActivityFilterByCollectionDto.Types.BID,
                            AuctionActivityFilterByCollectionDto.Types.CANCEL,
                            AuctionActivityFilterByCollectionDto.Types.FINISHED
                        )
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            }
        )
    }

    @ParameterizedTest
    @MethodSource("activityFilterData")
    fun `should find all auction activity`(
        auctions: List<Auction>,
        logs: List<LogEvent>,
        otherLogs: List<LogEvent>,
        filter: AuctionActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking<Unit> {
        saveAuction(*auctions.shuffled().toTypedArray())
        saveHistory(*logs.shuffled().toTypedArray())
        val allActivities = auctionActivityClient.getAuctionActivities(filter, null, null, sort).awaitSingle()

        assertThat(allActivities.items).hasSize(logs.size)

        allActivities.items.forEachIndexed { index, orderActivityDto ->
            checkAuctionActivityDto(orderActivityDto, logs[index])
        }
    }

    @ParameterizedTest
    @MethodSource("activityFilterData")
    fun `should find auction activity by pagination`(
        auctions: List<Auction>,
        logs: List<LogEvent>,
        otherLogs: List<LogEvent>,
        filter: AuctionActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {
        saveAuction(*auctions.shuffled().toTypedArray())
        saveHistory(*logs.shuffled().toTypedArray())

        val allActivities = mutableListOf<AuctionActivityDto>()

        var continuation: String? = null
        do {
            val activities = auctionActivityClient.getAuctionActivities(filter, continuation, 1, sort).awaitSingle()
            assertThat(activities.items).hasSizeLessThanOrEqualTo(1)

            allActivities.addAll(activities.items)
            continuation = activities.continuation
        } while (continuation != null)

        assertThat(allActivities).hasSize(logs.size)

        allActivities.forEachIndexed { index, orderActivityDto ->
            checkAuctionActivityDto(orderActivityDto, logs[index])
        }
    }

    private fun checkAuctionActivityDto(auctionActivityDto: AuctionActivityDto, history: LogEvent) {
        assertThat(auctionActivityDto.id).isEqualTo(history.id.toString())
        assertThat(auctionActivityDto.date.toEpochMilli()).isEqualTo((history.data as AuctionHistory).date.toEpochMilli())
    }

    private suspend fun saveHistory(vararg history: LogEvent) {
        history.forEach { auctionHistoryRepository.save(it).awaitFirst() }
    }

    private suspend fun saveAuction(vararg auction: Auction) {
        auction.forEach { auctionRepository.save(it) }
    }
}
