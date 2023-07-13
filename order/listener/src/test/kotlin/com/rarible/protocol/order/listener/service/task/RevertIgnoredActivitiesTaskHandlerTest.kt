package com.rarible.protocol.order.listener.service.task

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
class RevertIgnoredActivitiesTaskHandlerTest {
    private val exchangeHistoryRepository = mockk<ExchangeHistoryRepository>()
    private val publisher = mockk<ProtocolOrderPublisher>()
    private val orderActivityConverter = mockk<OrderActivityConverter>()

    val handler = RevertIgnoredActivitiesTaskHandler(
        exchangeHistoryRepository,
        publisher,
        orderActivityConverter,
    )

    @Test
    fun revert() = runBlocking<Unit> {
        val from = randomString()
        val source = HistorySource.OPEN_SEA

        val activities = listOf<OrderActivityDto>(
            mockk(),
            mockk(),
        ).onEach {
            coEvery {
                publisher.publish(it, any())
            } returns Unit
        }
        val events = listOf(
            createLogEvent(createOrderSideMatch()),
            createLogEvent(createOrderSideMatch())
        ).onEachIndexed { index, event ->
            coEvery {
                orderActivityConverter.convert(OrderActivityResult.History(event), true)
            } returns activities[index]

            coEvery {
                exchangeHistoryRepository.save(event.copy(status = EthereumBlockStatus.REVERTED))
            } returns Mono.just(event)
        }
        every {
            exchangeHistoryRepository.findIgnoredEvents(from, source)
        } returns flow { events.forEach { emit(it) } }

        val states = handler.runLongTask(from, source.name).toList()
        assertThat(states).hasSize(2)
        assertThat(states[0]).isEqualTo(events[0].id)
        assertThat(states[1]).isEqualTo(events[1].id)
    }
}