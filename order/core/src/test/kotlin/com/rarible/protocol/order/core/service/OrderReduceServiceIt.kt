package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.MongoOrderRepository
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Duration

@IntegrationTest
@Import(OrderReduceServiceIt.TestOrderRepository::class)
class OrderReduceServiceIt : AbstractIntegrationTest() {

    @Test
    fun `should calculate order for existed order`() = runBlocking<Unit> {
        val order = createOrder().copy(fill = EthUInt256.ZERO, cancelled = false)
        val hash = order.hash
        orderRepository.save(order)

        val sideMatchDate1 = nowMillis() + Duration.ofHours(2)
        val sideMatchDate2 = nowMillis() + Duration.ofHours(1)
        val cancelDate = nowMillis() + Duration.ofHours(3)

        val logEvents = prepareStorage(
            OrderSideMatch(
                hash = hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(1),
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate1,
                source = HistorySource.RARIBLE
            ),
            OrderSideMatch(
                hash = hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(2),
                maker = order.maker,
                make = order.make,
                take = order.take,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate2,
                source = HistorySource.RARIBLE
            ),
            OrderCancel(
                hash = hash,
                maker = order.maker,
                make = order.make,
                take = order.take,
                date = cancelDate,
                source = HistorySource.RARIBLE
            )
        )
        orderReduceService.onExchange(logEvents).awaitFirstOrNull()

        val updatedOrder = orderRepository.findById(hash)

        assertThat(updatedOrder?.fill).isEqualTo(EthUInt256.of(3))
        assertThat(updatedOrder?.cancelled).isEqualTo(true)
        assertThat(updatedOrder?.lastUpdateAt).isEqualTo(cancelDate)
    }


    @Test
    fun `should not change order lastUpdateAt if reduce past events`() = runBlocking<Unit> {
        val orderLastUpdateAt = nowMillis()
        val order = createOrder().copy(cancelled = false, lastUpdateAt = orderLastUpdateAt)
        val hash = order.hash
        orderRepository.save(order)

        val cancelDate = nowMillis() - Duration.ofHours(1)

        val logEvents = prepareStorage(
            OrderCancel(
                hash = hash,
                maker = order.maker,
                make = order.make,
                take = order.take,
                date = cancelDate,
                source = HistorySource.RARIBLE
            )
        )
        orderReduceService.onExchange(logEvents).awaitFirstOrNull()

        val updatedOrder = orderRepository.findById(hash)
        assertThat(updatedOrder?.cancelled).isEqualTo(true)
        assertThat(updatedOrder?.lastUpdateAt).isEqualTo(orderLastUpdateAt)
    }

    private suspend fun prepareStorage(vararg histories: OrderExchangeHistory): List<LogEvent> {
        return histories.mapIndexed { index, history ->
            exchangeHistoryRepository.save(
                LogEvent(
                    data = history, address = AddressFactory.create(), topic = word(), transactionHash = word(),
                    from = null, nonce = null, status = LogEventStatus.CONFIRMED, blockNumber = 1,
                    logIndex = 0, minorLogIndex = index, index = 0
                )
            ).awaitFirst()
        }
    }

    private fun word(): Word = Word.apply(RandomUtils.nextBytes(32))

    @TestConfiguration
    class TestOrderRepository {
        @Bean
        @Primary
        fun mongoOrderRepository(template: ReactiveMongoTemplate): MongoOrderRepository {
            return MongoOrderRepository(template)
        }
    }
}
