package com.rarible.protocol.order.core.service.pool.listener

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.data.randomPoolNftDeposit
import com.rarible.protocol.order.core.data.randomPoolNftWithdraw
import com.rarible.protocol.order.core.data.randomPoolSpotPriceUpdate
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.misc.orderStubEventMarks
import com.rarible.protocol.order.core.model.PoolActivityResult
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class PoolActivityListenerTest {
    private val orderPublisher = mockk<ProtocolOrderPublisher>()
    private val orderActivityConverter = mockk<OrderActivityConverter>()
    private val listener = PoolActivityListener(orderPublisher, orderActivityConverter)

    private companion object {
        @JvmStatic
        fun swapEvents(): Stream<Arguments> = Stream.of(
            Arguments.of(randomPoolTargetNftIn(), false),
            Arguments.of(randomPoolTargetNftOut(), true),
        )

        @JvmStatic
        fun otherEvents(): Stream<PoolHistory> = Stream.of(
            randomPoolNftWithdraw(),
            randomPoolNftDeposit(),
            randomPoolSpotPriceUpdate(),
            randomPoolDeltaUpdate(),
            randomPoolFeeUpdate()
        )
    }

    @ParameterizedTest
    @MethodSource("swapEvents")
    fun `should publish events`(event: PoolHistory, reverted: Boolean) = runBlocking<Unit> {
        val logEvent = createLogEvent(event).copy(status = if (reverted) EthereumBlockStatus.REVERTED else EthereumBlockStatus.CONFIRMED)

        val activityDto = mockk<OrderActivityDto>()

        coEvery { orderActivityConverter.convert(PoolActivityResult.History(logEvent), reverted) } returns activityDto
        coEvery { orderPublisher.publish(activityDto, any()) } returns Unit

        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify { orderActivityConverter.convert(PoolActivityResult.History(logEvent), reverted) }
        coVerify { orderPublisher.publish(activityDto, any()) }
    }

    @ParameterizedTest
    @MethodSource("otherEvents")
    fun `should not publish events`(event: PoolHistory) = runBlocking<Unit> {
        val logEvent = createLogEvent(event)
        listener.onPoolEvent(logEvent, orderStubEventMarks())

        coVerify(exactly = 0) { orderActivityConverter.convert(any(), any()) }
        coVerify(exactly = 0) { orderPublisher.publish(any<OrderActivityDto>(), any()) }
    }
}