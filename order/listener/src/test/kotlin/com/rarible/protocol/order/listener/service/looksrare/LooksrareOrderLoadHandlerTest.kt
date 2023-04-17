package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.model.LooksrareV2FetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareOrderLoadHandlerTest {
    private val looksrareOrderLoader = mockk<LooksrareOrderLoader>()
    private val aggregatorStateRepository = mockk<AggregatorStateRepository>()
    private val properties = LooksrareLoadProperties()

    private val handler = LooksrareOrderLoadHandler(
        looksrareOrderLoader,
        aggregatorStateRepository,
        properties
    )

    @Test
    fun `should get orders with init state`() = runBlocking<Unit> {
        val now = nowMillis().truncatedTo(ChronoUnit.SECONDS)
        val expectedListedAfter = now - properties.delay - properties.loadPeriod
        val expectedListedBefore = expectedListedAfter + properties.loadPeriod

        mockkStatic(Instant::class) {
            every{ Instant.now() } returns now
            coEvery { aggregatorStateRepository.getLooksrareState() } returns null
            coEvery { aggregatorStateRepository.save(any()) } returns Unit
            coEvery { looksrareOrderLoader.load(any()) } returns listOf(randomLooksrareOrder())

            handler.handle()

            coVerify {
                looksrareOrderLoader.load(
                    withArg { assertThat(it).isEqualTo(expectedListedAfter) },
                )
            }
            coVerify {
                aggregatorStateRepository.save(
                    withArg { assertThat(it.cursor).isEqualTo(expectedListedBefore.epochSecond.toString()) }
                )
            }
        }
    }

    @Test
    fun `should get orders with saved state`() = runBlocking<Unit> {
        val expectedListedAfter = nowMillis().truncatedTo(ChronoUnit.SECONDS) - Duration.ofDays(1)
        val expectedListedBefore = expectedListedAfter + properties.loadPeriod

        coEvery { aggregatorStateRepository.getLooksrareV2State() } returns LooksrareV2FetchState.withCreatedAfter(expectedListedAfter)
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareOrderLoader.load(any()) } returns listOf(randomLooksrareOrder())

        handler.handle()

        coVerify {
            looksrareOrderLoader.load(
                withArg { assertThat(it).isEqualTo(expectedListedAfter) },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat(it.cursor).isEqualTo(expectedListedBefore.epochSecond.toString()) }
            )
        }
    }
}