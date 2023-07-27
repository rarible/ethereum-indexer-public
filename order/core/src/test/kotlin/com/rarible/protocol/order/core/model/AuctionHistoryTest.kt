package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.randomAuctionCreated
import com.rarible.protocol.order.core.data.randomBidPlaced
import com.rarible.protocol.order.core.data.randomCanceled
import com.rarible.protocol.order.core.data.randomFinished
import com.rarible.protocol.order.core.misc.MAPPER
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class AuctionHistoryTest {
    private companion object {
        private val auctionHistories = listOf<Pair<AuctionHistory, Class<*>>>(
            randomAuctionCreated() to OnChainAuction::class.java,
            randomBidPlaced() to BidPlaced::class.java,
            randomFinished() to AuctionFinished::class.java,
            randomCanceled() to AuctionCancelled::class.java
        )

        @JvmStatic
        fun orderExchangeHistoryStream(): Stream<Arguments> = run {
            auctionHistories.stream().map { Arguments.of(it.first, it.second) }
        }
    }

    @ParameterizedTest
    @MethodSource("orderExchangeHistoryStream")
    fun `serialize and deserialize - ok`(auctionHistory: AuctionHistory, auctionHistoryClass: Class<*>) {
        val json = MAPPER.writeValueAsString(auctionHistory)
        println(json)
        val deserialized = MAPPER.readValue(json, auctionHistoryClass)
        Assertions.assertThat(deserialized).isEqualTo(auctionHistory)
    }
}
