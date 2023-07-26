package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.model.SeaportFetchState
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

internal class SeaportOrderLoadHandlerTest {
    private val seaportOrderLoader = mockk<SeaportOrderLoader>()
    private val legacyOpenSeaFetchStateRepository = mockk<OpenSeaFetchStateRepository>()
    private val aggregatorStateRepository = mockk<AggregatorStateRepository>()
    private val properties = SeaportLoadProperties(pollingPeriod = Duration.ZERO)

    private val handler = SeaportOrderLoadHandler(
        seaportOrderLoader,
        legacyOpenSeaFetchStateRepository,
        aggregatorStateRepository,
        properties
    )

    @Test
    fun `should get init state from legacy state`() = runBlocking<Unit> {
        coEvery { aggregatorStateRepository.getSeaportState() } returns null
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { legacyOpenSeaFetchStateRepository.get(SeaportOrderLoadHandler.STATE_ID_PREFIX) } returns OpenSeaFetchState(cursor = "current", listedAfter = 1)
        coEvery { seaportOrderLoader.load("current", false) } returns SeaportOrders(next = "next", previous = "previous", orders = emptyList())

        handler.handle()

        coVerify { aggregatorStateRepository.getSeaportState() }
        coVerify { legacyOpenSeaFetchStateRepository.get(SeaportOrderLoadHandler.STATE_ID_PREFIX) }

        coVerify {
            aggregatorStateRepository.save(withArg {
                assertThat(it.cursor).isEqualTo("previous")
            })
        }
    }

    @Test
    fun `should use previous state if it not completed`() = runBlocking<Unit> {
        coEvery { aggregatorStateRepository.getSeaportState() } returns SeaportFetchState(cursor = "current")
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { seaportOrderLoader.load("current", false) } returns SeaportOrders(next = "next", previous = null, orders = emptyList())

        handler.handle()

        coVerify {
            aggregatorStateRepository.save(withArg {
                assertThat(it.cursor).isEqualTo("current")
            })
        }
    }
}
