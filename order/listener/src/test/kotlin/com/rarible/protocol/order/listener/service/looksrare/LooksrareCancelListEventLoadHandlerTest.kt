package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.common.nowMillis
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2CancelListEventFetchState
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
internal class LooksrareCancelListEventLoadHandlerTest {
    @InjectMockKs
    private lateinit var looksrareCancelListEventLoadHandler: LooksrareCancelListEventLoadHandler

    @MockK
    private lateinit var looksrareCancelListEventLoader: LooksrareCancelListEventLoader

    @MockK
    private lateinit var aggregatorStateRepository: AggregatorStateRepository

    @SpyK
    private var properties = LooksrareLoadProperties()

    @Test
    fun `should get orders with init state`() = runBlocking<Unit> {
        val next = LooksrareV2Cursor(Instant.now())
        coEvery { aggregatorStateRepository.getLooksrareV2CancelListEventState() } returns null
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareCancelListEventLoader.load(any()) } returns Result(next, 1)

        looksrareCancelListEventLoadHandler.handle()

        coVerify {
            looksrareCancelListEventLoader.load(
                withArg {
                    assertThat(it.createdAfter).isBeforeOrEqualTo(Instant.now() - Duration.ofHours(1))
                },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat((it as LooksrareV2CancelListEventFetchState).cursorObj).isEqualTo(next) }
            )
        }
    }

    @Test
    fun `should get orders with saved state`() = runBlocking<Unit> {
        val expectedCreatedAfter = LooksrareV2Cursor(nowMillis().truncatedTo(ChronoUnit.SECONDS) - Duration.ofDays(1))
        val next = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))

        coEvery { aggregatorStateRepository.getLooksrareV2CancelListEventState() } returns
            LooksrareV2CancelListEventFetchState(cursorObj = expectedCreatedAfter)
        coEvery { aggregatorStateRepository.save(any()) } returns Unit
        coEvery { looksrareCancelListEventLoader.load(any()) } returns Result(next, 0)

        looksrareCancelListEventLoadHandler.handle()

        coVerify {
            looksrareCancelListEventLoader.load(
                withArg { assertThat(it).isEqualTo(expectedCreatedAfter) },
            )
        }
        coVerify {
            aggregatorStateRepository.save(
                withArg { assertThat((it as LooksrareV2CancelListEventFetchState).cursorObj).isEqualTo(next) }
            )
        }
    }
}
