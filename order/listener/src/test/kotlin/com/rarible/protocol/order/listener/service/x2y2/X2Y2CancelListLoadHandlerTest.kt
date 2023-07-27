package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.model.X2Y2CancelListEventFetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2EventLoadProperties
import com.rarible.x2y2.client.model.ApiListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

internal class X2Y2CancelListLoadHandlerTest {
    private val stateRepository = mockk<AggregatorStateRepository>()
    private val x2y2CancelListEventLoader = mockk<X2Y2CancelListEventLoader>()
    private val properties = X2Y2EventLoadProperties(startCursor = 1644510383, pollingPeriod = Duration.ZERO)

    private val handler = X2Y2CancelEventsLoadHandler(
        stateRepository,
        x2y2CancelListEventLoader,
        properties
    )

    @Test
    fun `should get init state and save it`() = runBlocking<Unit> {
        coEvery { stateRepository.getX2Y2CancelListEventState() } returns null
        coEvery { x2y2CancelListEventLoader.load("WzE2NDQ1MTAzODNd") } returns ApiListResponse(next = "next", data = emptyList(), success = true)
        coEvery { stateRepository.save(any()) } returns mockk()

        handler.handle()

        coVerify { stateRepository.getX2Y2CancelListEventState() }
        coVerify { x2y2CancelListEventLoader.load("WzE2NDQ1MTAzODNd") }
        coVerify { stateRepository.save(withArg {
            assertThat(it.cursor).isEqualTo("next")
        }) }
    }

    @Test
    fun `should save current state if cursor not use fully`() = runBlocking<Unit> {
        val state = X2Y2CancelListEventFetchState(cursor = "current")
        coEvery { stateRepository.getX2Y2CancelListEventState() } returns state
        coEvery { x2y2CancelListEventLoader.load(state.cursor) } returns ApiListResponse(next = null, data = emptyList(), success = true)
        coEvery { stateRepository.save(any()) } returns mockk()

        handler.handle()

        coVerify { stateRepository.save(withArg {
            assertThat(it.cursor).isEqualTo(state.cursor)
        }) }
    }
}
