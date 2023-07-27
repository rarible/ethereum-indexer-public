package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsStateAsset
import com.rarible.protocol.nft.core.model.SuspiciousUpdateResult
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.listener.configuration.UpdateSuspiciousItemsHandlerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class UpdateSuspiciousItemsHandlerTest {
    private val stateService = mockk<UpdateSuspiciousItemsStateService>()
    private val suspiciousItemsService = mockk<SuspiciousItemsService>()
    private val properties = mockk<UpdateSuspiciousItemsHandlerProperties>()
    private val listenerMetrics = mockk<NftListenerMetricsFactory> {
        every { onSuspiciousCollectionsGet(any()) } returns Unit
    }
    private val handler = UpdateSuspiciousItemsHandler(
        stateService,
        suspiciousItemsService,
        properties,
        listenerMetrics
    )

    @Test
    fun `handle - from saved state`() = runBlocking<Unit> {
        val runningStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset() }
        val finishedStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset() }
        val state = UpdateSuspiciousItemsState(Instant.now(), runningStateAssets + finishedStateAssets)

        coEvery { stateService.getState() } returns state
        coEvery { stateService.save(any()) } returns Unit

        every { properties.chunkSize } returns 5

        runningStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns SuspiciousUpdateResult.Success(it)
        }
        finishedStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns SuspiciousUpdateResult.Success(it.copy(cursor = null))
        }

        handler.handle()

        coVerify {
            stateService.save(withArg {
                assertThat(it.statedAt).isEqualTo(state.statedAt)
                assertThat(it.assets).containsExactlyInAnyOrderElementsOf(runningStateAssets)
            })
        }
        coVerify(exactly = 10) {
            suspiciousItemsService.update(any())
        }
    }

    @Test
    fun `handle - save all states if error`() = runBlocking<Unit> {
        val runningStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset() }
        val finishedStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset().copy(cursor = null) }
        val state = UpdateSuspiciousItemsState(Instant.now(), runningStateAssets + finishedStateAssets)

        coEvery { stateService.getState() } returns state
        coEvery { stateService.save(any()) } returns Unit

        every { properties.chunkSize } returns 5

        runningStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns SuspiciousUpdateResult.Fail(it)
        }
        finishedStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns SuspiciousUpdateResult.Fail(it)
        }
        handler.handle()

        coVerify {
            stateService.save(withArg {
                assertThat(it.statedAt).isEqualTo(state.statedAt)
                assertThat(it.assets).containsExactlyInAnyOrderElementsOf(runningStateAssets + finishedStateAssets)
            })
        }
    }

    @Test
    fun `handle - from init state`() = runBlocking<Unit> {
        val state = UpdateSuspiciousItemsState(Instant.now(), listOf(randomUpdateSuspiciousItemsStateAsset()))
        coEvery { stateService.getState() } returns null
        coEvery { stateService.getInitState(any()) } returns state
        coEvery { stateService.save(any()) } returns Unit
        every { properties.chunkSize } returns 5
        coEvery { suspiciousItemsService.update(state.assets.single()) } returns SuspiciousUpdateResult.Success(state.assets.single())

        handler.handle()

        coVerify(exactly = 1) {
            stateService.getInitState(any())
        }
        coVerify(exactly = 1) {
            suspiciousItemsService.update(any())
        }
    }

    @Test
    fun `delay - if not yet next start`() = runBlocking<Unit> {
        val statedAt = Instant.now()
        val state = UpdateSuspiciousItemsState(statedAt, emptyList())

        coEvery { stateService.getState() } returns state
        coEvery { stateService.save(state) } returns Unit
        every { properties.handlePeriod } returns Duration.ofDays(1)
        every { properties.awakePeriod } returns Duration.ZERO

        handler.handle()

        coVerify(exactly = 0) {
            stateService.getInitState(any())
            suspiciousItemsService.update(any())
        }
    }

    @Test
    fun `update nothing - re-init state`() = runBlocking<Unit> {
        val statedAt = Instant.now() - Duration.ofMinutes(1)
        val state = UpdateSuspiciousItemsState(statedAt, emptyList())
        val initState = randomUpdateSuspiciousItemsState()

        coEvery { stateService.getState() } returns state
        coEvery { stateService.getInitState(any()) } returns initState
        coEvery { stateService.save(initState) } returns Unit
        every { properties.handlePeriod } returns Duration.ofSeconds(30)

        handler.handle()

        coVerify(exactly = 0) {
            suspiciousItemsService.update(any())
        }
        coVerify(exactly = 1) {
            stateService.save(initState)
        }
    }
}
