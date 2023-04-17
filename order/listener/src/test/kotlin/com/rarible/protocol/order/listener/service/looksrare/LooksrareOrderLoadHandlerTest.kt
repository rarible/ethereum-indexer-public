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
        val next = now - Duration.ofHours(1)

        mockkStatic(Instant::class) {
            every{ Instant.now() } returns now
            coEvery { aggregatorStateRepository.getLooksrareV2State() } returns null
            coEvery { aggregatorStateRepository.save(any()) } returns Unit
            coEvery { looksrareOrderLoader.load(any()) } returns listOf(randomLooksrareOrder().copy(createdAt = next))

            handler.handle()

            coVerify {
                looksrareOrderLoader.load(
                    withArg { assertThat(it).isEqualTo(next) },
                )
            }
            coVerify {
                aggregatorStateRepository.save(
                    withArg { assertThat(it.cursor).isEqualTo(next.epochSecond.toString()) }
                )
            }
        }
    }

    @Test
    fun `should get orders with saved state`() = runBlocking<Unit> {
        val expectedCreatedAfter = nowMillis().truncatedTo(ChronoUnit.SECONDS) - Duration.ofDays(1)
        val next = Instant.now() - Duration.ofHours(1)

        coEvery { aggregatorStateRepository.getLooksrareV2State() } returns LooksrareV2FetchState.withCreatedAfter(expectedCreatedAfter)
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareOrderLoader.load(any()) } returns listOf(randomLooksrareOrder().copy(createdAt = next))

        handler.handle()

        coVerify {
            looksrareOrderLoader.load(
                withArg { assertThat(it).isEqualTo(expectedCreatedAfter) },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat(it.cursor).isEqualTo(next.epochSecond.toString()) }
            )
        }
    }
}