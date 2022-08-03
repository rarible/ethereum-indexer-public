package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.repository.looksrare.LooksrareFetchStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.listener.data.randomLooksrareOrder
import com.rarible.protocol.order.listener.service.looksrare.LooksrareOrderLoadHandler.Companion.STATE_ID_PREFIX
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

internal class LooksrareOrderLoadHandlerTest {
    private val looksrareOrderLoader = mockk<LooksrareOrderLoader>()
    private val looksrareFetchStateRepository = mockk<LooksrareFetchStateRepository>()
    private val properties = LooksrareLoadProperties()

    private val handler = LooksrareOrderLoadHandler(
        looksrareOrderLoader,
        looksrareFetchStateRepository,
        properties
    )

    @Test
    fun `should get orders with init state`() = runBlocking<Unit> {
        val now = Instant.now()
        val expectedListedAfter = now - properties.delay - properties.loadPeriod
        val expectedListedBefore = expectedListedAfter + properties.loadPeriod

        mockkStatic(Instant::class) {
            every{ Instant.now() } returns now
            coEvery { looksrareFetchStateRepository.get(STATE_ID_PREFIX) } returns null
            coEvery { looksrareFetchStateRepository.save(any()) } returns Unit
            coEvery { looksrareOrderLoader.load(any(), any()) } returns listOf(randomLooksrareOrder())

            handler.handle()

            coVerify {
                looksrareOrderLoader.load(
                    withArg { assertThat(it).isEqualTo(expectedListedAfter) },
                    withArg { assertThat(it).isEqualTo(expectedListedBefore) }
                )
            }
            coVerify {
                looksrareFetchStateRepository.save(
                    withArg { assertThat(it.listedAfter).isEqualTo(expectedListedBefore) }
                )
            }
        }
    }

    @Test
    fun `should get orders with saved state`() = runBlocking<Unit> {
        val expectedListedAfter = Instant.now() - Duration.ofDays(1)
        val expectedListedBefore = expectedListedAfter + properties.loadPeriod

        coEvery { looksrareFetchStateRepository.get(STATE_ID_PREFIX) } returns LooksrareFetchState(expectedListedAfter)
        coEvery { looksrareFetchStateRepository.save(any()) } returns Unit
        coEvery { looksrareOrderLoader.load(any(), any()) } returns listOf(randomLooksrareOrder())

        handler.handle()

        coVerify {
            looksrareOrderLoader.load(
                withArg { assertThat(it).isEqualTo(expectedListedAfter) },
                withArg { assertThat(it).isEqualTo(expectedListedBefore) }
            )
        }
        coVerify {
            looksrareFetchStateRepository.save(
                withArg { assertThat(it.listedAfter).isEqualTo(expectedListedBefore) }
            )
        }
    }
}