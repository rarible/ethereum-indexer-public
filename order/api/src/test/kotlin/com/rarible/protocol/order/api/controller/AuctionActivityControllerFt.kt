package com.rarible.protocol.order.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.model.tokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@IntegrationTest
class AuctionActivityControllerFt : AbstractIntegrationTest() {
    internal companion object {
        private val now = Instant.ofEpochSecond(Instant.now().epochSecond)

        @JvmStatic
        fun activityFilterData() = Stream.of(
            run { // all
                val data = randomAuctionCreated()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.CREATED)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.hash)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.BID)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val data = randomCanceled()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.CANCEL)),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val data = randomFinished()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.FINISHED)),
                    ActivitySortDto.LATEST_FIRST
                )
            },

            // user
            run {
                val data = randomAuctionCreated()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterByUserDto(
                        data.seller,
                        listOf(AuctionActivityFilterByUserDto.Types.CREATED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val auction = randomAuction()
                val data = randomBidPlaced(auction.hash)
                Arguments.of(
                    listOf(auction),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterByUserDto(
                        data.buyer!!,
                        listOf(AuctionActivityFilterByUserDto.Types.BID)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            }, run {
                val data = randomFinished()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterByUserDto(
                        data.seller,
                        listOf(AuctionActivityFilterByUserDto.Types.FINISHED)
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },

            // item
            run {
                val data = randomAuctionCreated()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterByItemDto(data.sell.type.token, data.sell.type.tokenId!!.value, listOf(AuctionActivityFilterByItemDto.Types.CREATED)),
                    ActivitySortDto.LATEST_FIRST
                )
            }
            // we can't find bid by item, only by auction id
//            , run {
//                val auction = randomAuction()
//                val data = randomBidPlaced(auction.hash)
//                Arguments.of(
//                    listOf(auction),
//                    listOf(createAuctionLogEvent(data)),
//                    emptyList<LogEvent>(),
//                    AuctionActivityFilterByItemDto(auction.sell.type.token, auction.sell.type.tokenId!!.value, listOf(AuctionActivityFilterByItemDto.Types.BID)),
//                    ActivitySortDto.LATEST_FIRST
//                )
//            }
//            , run {
//                val data = randomCanceled()
//                Arguments.of(
//                    emptyList<Auction>(),
//                    listOf(createAuctionLogEvent(data)),
//                    emptyList<LogEvent>(),
//                    AuctionActivityFilterByItemDto(data.sell.type.token, data.sell.type.tokenId!!.value, listOf(AuctionActivityFilterByItemDto.Types.CANCEL)),
//                    ActivitySortDto.LATEST_FIRST
//                )
//            },
            , run {
                val data = randomFinished()
                Arguments.of(
                    emptyList<Auction>(),
                    listOf(createAuctionLogEvent(data)),
                    emptyList<LogEvent>(),
                    AuctionActivityFilterByItemDto(data.sell.type.token, data.sell.type.tokenId!!.value, listOf(AuctionActivityFilterByItemDto.Types.FINISHED)),
                    ActivitySortDto.LATEST_FIRST
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
        val allActivities = auctionActivityClient.getAuctionActivities(filter, null, null, null).awaitSingle()

        assertThat(allActivities.items).hasSize(logs.size)

        allActivities.items.forEachIndexed { index, orderActivityDto ->
            checkAuctionActivityDto(orderActivityDto, logs[index])
        }
    }

    @Test
    fun `should find`() = runBlocking {

        val contract = randomAddress()
        val auctionId = EthUInt256.ONE
        val hash = Auction.raribleV1HashKey(contract, auctionId)
        val auction = randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(0))
        saveAuction(auction)

        val data = randomAuctionCreated()
        val log = createAuctionLogEvent(data)
        saveHistory(log)

//        createAuctionLogEvent(randomBidPlaced()
//            .copy(hash = hash, bidValue = BigDecimal.valueOf(5), bid = randomBid().copy(amount = EthUInt256.of(5))))

        val filter = AuctionActivityFilterAllDto(listOf(AuctionActivityFilterAllDto.Types.CREATED))
        val activities = auctionActivityClient.getAuctionActivities(filter, null, null, null).awaitSingle()
        println(activities)
    }

    private fun checkAuctionActivityDto(auctionActivityDto: AuctionActivityDto, history: LogEvent) {
        assertThat(auctionActivityDto.id).isEqualTo(history.id.toString())
        assertThat(auctionActivityDto.date.toEpochMilli()).isEqualTo((history.data as AuctionHistory).date.toEpochMilli())
    }

    private suspend fun saveHistory(vararg history: LogEvent) {
        history.forEach { auctionHistoryRepository.save(it).awaitFirst() }
    }

    private suspend fun saveVersion(vararg version: OrderVersion) {
        version.forEach { orderVersionRepository.save(it).awaitFirst() }
    }


    private suspend fun saveAuction(vararg auction: Auction) {
        auction.forEach { auctionRepository.save(it) }
    }
}
