package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.data.randomAuction
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

@IntegrationTest
class AuctionSearchFt : AbstractIntegrationTest() {
    companion object {
        @JvmStatic
        fun auctions(): Stream<Arguments> = run {
            val now = nowMillis()

            Stream.of(
                Arguments.of(
                    AuctionSortDto.LAST_UPDATE_ASC,
                    listOf(
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(0)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    )
                ),
                Arguments.of(
                    AuctionSortDto.LAST_UPDATE_DESC,
                    listOf(
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(0))
                    )
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("auctions")
    fun `should find all auctions with continuation`(
        sort: AuctionSortDto,
        auctions: List<Auction>
    ) = runBlocking<Unit> {
        saveAuction(*auctions.shuffled().toTypedArray())

        Wait.waitAssert {
            val allAuctions = mutableListOf<AuctionDto>()

            var continuation: String? = null
            do {
                val result = auctionClient.getAuctionsAll(sort, null, null, null, continuation, 2).awaitFirst()
                assertThat(result.auctions).hasSizeLessThanOrEqualTo(2)

                allAuctions.addAll(result.auctions)
                continuation = result.continuation
            } while (continuation != null)

            assertThat(allAuctions).hasSize(auctions.size)

            allAuctions.forEachIndexed { index, orderDto ->
                checkAuctionDto(orderDto, auctions[index])
            }
        }
    }

    private fun checkAuctionDto(auctionDto: AuctionDto, auction: Auction) {
        assertThat(auctionDto.hash).isEqualTo(auction.hash)
    }

    private suspend fun saveAuction(vararg auction: Auction) {
        auction.forEach { auctionRepository.save(it) }
    }
}
