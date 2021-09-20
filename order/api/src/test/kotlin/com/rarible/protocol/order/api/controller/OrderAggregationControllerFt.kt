package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.api.data.*
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.time.Duration

@IntegrationTest
class OrderAggregationControllerFt : AbstractIntegrationTest() {
    @Test
    fun `should aggregate nft sales by maker`() = runBlocking<Unit> {
        val now = nowMillis()
        val startDate = now - Duration.ofMinutes(10)
        val endDate = startDate + Duration.ofMinutes(5)

        val maker1 = AddressFactory.create()
        val maker2 = AddressFactory.create()

        val maker1history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMaker(maker1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(5))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMaker(maker1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc1155SellSideMatch()
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withMaker(maker1)
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            createLogEvent(
                orderErc1155SellSideMatch()
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withMaker(maker1)
                    .withDate(startDate + Duration.ofMinutes(3))
            ),
            //-------
            createLogEvent(
                orderErc1155SellSideMatch()
                    .withMaker(maker1)
                    .withDate(startDate + Duration.ofMinutes(6))
            )
        )
        val maker2history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMaker(maker2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(11))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMaker(maker2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc1155SellSideMatch()
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withMaker(maker2)
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            //-------
            createLogEvent(
                orderErc1155SellSideMatch()
                    .withMaker(maker2)
                    .withDate(startDate + Duration.ofMinutes(7))
            )
        )

        saveHistory(*maker1history.toTypedArray())
        saveHistory(*maker2history.toTypedArray())

        val aggregation = orderAggregationApi.aggregateNftSellByMaker(
            startDate.toEpochMilli(),
            endDate.toEpochMilli(),
            null,
            null
        ).collectList().awaitFirst()

        assertThat(aggregation).hasSize(2)

        val maker1Aggregation = aggregation[0]
        assertThat(maker1Aggregation.address).isEqualTo(maker1)
        assertThat(maker1Aggregation.sum.longValueExact()).isEqualTo(7)
        assertThat(maker1Aggregation.count).isEqualTo(3)

        val maker2Aggregation = aggregation[1]
        assertThat(maker2Aggregation.address).isEqualTo(maker2)
        assertThat(maker2Aggregation.sum.longValueExact()).isEqualTo(5)
        assertThat(maker2Aggregation.count).isEqualTo(2)
    }

    @Test
    fun `should skip cancel events for nft purchases by taker`() = runBlocking<Unit> {
        val now = nowMillis()
        val startDate = now - Duration.ofMinutes(10)
        val endDate = startDate + Duration.ofMinutes(10)

        val taker = AddressFactory.create()

        val taker1history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(5))
            ),
            createLogEvent(
                orderErc1155SellCancel()
                    .withDate(startDate + Duration.ofMinutes(5))
            )
        )
        saveHistory(*taker1history.toTypedArray())

        val aggregation = orderAggregationApi.aggregateNftPurchaseByTaker(
            startDate.toEpochMilli(),
            endDate.toEpochMilli(),
            null,
            null
        ).collectList().awaitFirst()

        assertThat(aggregation).hasSize(1)

        val taker1Aggregation = aggregation[0]
        assertThat(taker1Aggregation.address).isEqualTo(taker)
        assertThat(taker1Aggregation.sum.longValueExact()).isEqualTo(2)
        assertThat(taker1Aggregation.count).isEqualTo(1)
    }

    @Test
    fun `should skip cancel events for nft sell by maker`() = runBlocking<Unit> {
        val now = nowMillis()
        val startDate = now - Duration.ofMinutes(10)
        val endDate = startDate + Duration.ofMinutes(10)

        val maker = AddressFactory.create()

        val maker1history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMaker(maker)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(5))
            ),
            createLogEvent(
                orderErc1155SellCancel()
                    .withMaker(maker)
                    .withDate(startDate + Duration.ofMinutes(5))
            )
        )
        saveHistory(*maker1history.toTypedArray())

        val aggregation = orderAggregationApi.aggregateNftSellByMaker(
            startDate.toEpochMilli(),
            endDate.toEpochMilli(),
            null,
            null
        ).collectList().awaitFirst()

        assertThat(aggregation).hasSize(1)

        val maker1Aggregation = aggregation[0]
        assertThat(maker1Aggregation.address).isEqualTo(maker)
        assertThat(maker1Aggregation.sum.longValueExact()).isEqualTo(2)
        assertThat(maker1Aggregation.count).isEqualTo(1)
    }


    @Test
    fun `should aggregate nft purchases by taker`() = runBlocking<Unit> {
        val now = nowMillis()
        val startDate = now - Duration.ofMinutes(10)
        val endDate = startDate + Duration.ofMinutes(5)

        val taker1 = AddressFactory.create()
        val taker2 = AddressFactory.create()

        val taker1history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(5))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker1)
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(3))
            ),
            //-------
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker1)
                    .withDate(startDate + Duration.ofMinutes(6))
            )
        )
        val taker2history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(11))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker2)
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            //-------
            createLogEvent(
                orderErc721SellSideMatch()
                    .withTaker(taker2)
                    .withDate(startDate + Duration.ofMinutes(7))
            )
        )

        saveHistory(*taker1history.toTypedArray())
        saveHistory(*taker2history.toTypedArray())

        val aggregation = orderAggregationApi.aggregateNftPurchaseByTaker(
            startDate.toEpochMilli(),
            endDate.toEpochMilli(),
            null,
            null
        ).collectList().awaitFirst()

        assertThat(aggregation).hasSize(2)

        val taker1Aggregation = aggregation[0]
        assertThat(taker1Aggregation.address).isEqualTo(taker1)
        assertThat(taker1Aggregation.sum.longValueExact()).isEqualTo(7)
        assertThat(taker1Aggregation.count).isEqualTo(3)

        val taker2Aggregation = aggregation[1]
        assertThat(taker2Aggregation.address).isEqualTo(taker2)
        assertThat(taker2Aggregation.sum.longValueExact()).isEqualTo(5)
        assertThat(taker2Aggregation.count).isEqualTo(2)
    }

    @Test
    fun `should aggregate nft purchases by collection`() = runBlocking<Unit> {
        val now = nowMillis()
        val startDate = now - Duration.ofMinutes(10)
        val endDate = startDate + Duration.ofMinutes(5)

        val collection1 = AddressFactory.create()
        val collection2 = AddressFactory.create()

        val collection1history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(5))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection1)
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection1)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(3))
            ),
            //-------
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection1)
                    .withDate(startDate + Duration.ofMinutes(6))
            )
        )
        val collection2history = listOf(
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate - Duration.ofMinutes(11))
            ),
            // Calculated history items
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection2)
                    .withTakeUsd(BigDecimal.valueOf(2))
                    .withDate(startDate + Duration.ofMinutes(1))
            ),
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection2)
                    .withTakeUsd(BigDecimal.valueOf(3))
                    .withDate(startDate + Duration.ofMinutes(2))
            ),
            //-------
            createLogEvent(
                orderErc721SellSideMatch()
                    .withMakeToken(collection2)
                    .withDate(startDate + Duration.ofMinutes(7))
            )
        )

        saveHistory(*collection1history.toTypedArray())
        saveHistory(*collection2history.toTypedArray())

        val aggregation = orderAggregationApi.aggregateNftPurchaseBuyCollection(
            startDate.toEpochMilli(),
            endDate.toEpochMilli(),
            null,
            null
        ).collectList().awaitFirst()

        assertThat(aggregation).hasSize(2)

        val collection1Aggregation = aggregation[0]
        assertThat(collection1Aggregation.address).isEqualTo(collection1)
        assertThat(collection1Aggregation.sum.longValueExact()).isEqualTo(7)
        assertThat(collection1Aggregation.count).isEqualTo(3)

        val collection2Aggregation = aggregation[1]
        assertThat(collection2Aggregation.address).isEqualTo(collection2)
        assertThat(collection2Aggregation.sum.longValueExact()).isEqualTo(5)
        assertThat(collection2Aggregation.count).isEqualTo(2)
    }

    private suspend fun saveHistory(vararg history: LogEvent) {
        history.forEach { exchangeHistoryRepository.save(it).awaitFirst() }
    }
}
