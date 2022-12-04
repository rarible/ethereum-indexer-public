package com.rarible.protocol.order.listener.service.descriptors.event

import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.misc.toReversedEthereumLogRecord
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OnExchangeLogEventListenerTest {
    private val eventPublisher = mockk<ProtocolOrderPublisher>()
    private val orderActivityConverter = mockk<OrderActivityConverter>()

    private val onExchangeLogEventListener = OnExchangeLogEventListener(eventPublisher, orderActivityConverter)

    @Test
    fun `should publish confirm activity`() = runBlocking<Unit> {
        val logEvent = createLogEvent(createOrderSideMatch().copy(side = OrderSide.LEFT))
        val expectedPublishedActivity = mockk<OrderActivityDto>()

        coEvery { orderActivityConverter.convert(any(), eq(false)) } returns expectedPublishedActivity
        coEvery { eventPublisher.publish(expectedPublishedActivity) } returns Unit
        onExchangeLogEventListener.onLogEvent(logEvent).awaitFirstOrNull()

        coVerify {
            orderActivityConverter.convert(
                withArg {
                        assertThat(it).isInstanceOf(OrderActivityResult.History::class.java)
                    assertThat((it as OrderActivityResult.History).value).isEqualTo(logEvent.toReversedEthereumLogRecord())
                },
                eq(false)
            )
            eventPublisher.publish(expectedPublishedActivity)
        }
    }

    @Test
    fun `should publish reverted activity`() = runBlocking<Unit> {
        val logEvent = createLogEvent(createOrderSideMatch().copy(side = OrderSide.LEFT), status = LogEventStatus.REVERTED)
        val expectedPublishedActivity = mockk<OrderActivityDto>()

        coEvery { orderActivityConverter.convert(any(), eq(true)) } returns expectedPublishedActivity
        coEvery { eventPublisher.publish(expectedPublishedActivity) } returns Unit
        onExchangeLogEventListener.onRevertedLogEvent(logEvent).awaitFirstOrNull()

        coVerify {
            orderActivityConverter.convert(
                withArg {
                    assertThat(it).isInstanceOf(OrderActivityResult.History::class.java)
                    assertThat((it as OrderActivityResult.History).value).isEqualTo(logEvent.toReversedEthereumLogRecord())
                },
                eq(true)
            )
            eventPublisher.publish(expectedPublishedActivity)
        }
    }
}