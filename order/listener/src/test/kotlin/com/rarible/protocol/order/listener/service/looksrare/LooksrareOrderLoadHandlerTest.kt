package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.LooksrareV2FetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class LooksrareOrderLoadHandlerTest {
    private val looksrareOrderLoader = mockk<LooksrareOrderLoader>()
    private val aggregatorStateRepository = mockk<AggregatorStateRepository>()
    private val properties = LooksrareLoadProperties(pollingPeriod = Duration.ZERO)

    private val handler = LooksrareOrderLoadHandler(
        looksrareOrderLoader,
        aggregatorStateRepository,
        properties
    )

    @Test
    fun `should get orders with init state`() = runBlocking<Unit> {
        val next = LooksrareV2Cursor(Instant.now())
        coEvery { aggregatorStateRepository.getLooksrareV2State() } returns null
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareOrderLoader.load(any()) } returns Result(next, 1)

        handler.handle()

        coVerify {
            looksrareOrderLoader.load(
                withArg { assertThat(it.createdAfter).isBeforeOrEqualTo(Instant.now() - Duration.ofHours(1)) },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat((it as LooksrareV2FetchState).cursorObj).isEqualTo(next) }
            )
        }
    }

    @Test
    fun `should get orders with saved state`() = runBlocking<Unit> {
        val expectedCreatedAfter = LooksrareV2Cursor(nowMillis().truncatedTo(ChronoUnit.SECONDS) - Duration.ofDays(1))
        val next = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))

        coEvery { aggregatorStateRepository.getLooksrareV2State() } returns LooksrareV2FetchState(cursorObj = expectedCreatedAfter)
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareOrderLoader.load(any()) } returns Result(next, 0)

        handler.handle()

        coVerify {
            looksrareOrderLoader.load(
                withArg { assertThat(it).isEqualTo(expectedCreatedAfter) },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat((it as LooksrareV2FetchState).cursorObj).isEqualTo(next) }
            )
        }
    }
}
