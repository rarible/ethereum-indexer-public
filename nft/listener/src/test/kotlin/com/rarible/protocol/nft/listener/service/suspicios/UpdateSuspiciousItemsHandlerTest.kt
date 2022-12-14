package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsStateAsset
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.listener.configuration.UpdateSuspiciousItemsHandlerProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class UpdateSuspiciousItemsHandlerTest {
    private val stateService = mockk<UpdateSuspiciousItemsStateService>()
    private val suspiciousItemsService = mockk<SuspiciousItemsService>()
    private val properties = mockk<UpdateSuspiciousItemsHandlerProperties>()

    private val handler = UpdateSuspiciousItemsHandler(stateService, suspiciousItemsService, properties)

    @Test
    fun `handle - from saved state`() = runBlocking<Unit> {
        val runningStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset() }
        val finishedStateAssets = (1..5).map { randomUpdateSuspiciousItemsStateAsset() }
        val state = UpdateSuspiciousItemsState(Instant.now(), runningStateAssets + finishedStateAssets)

        coEvery { stateService.getState() } returns state
        coEvery { stateService.save(any()) } answers { it.invocation.args.first() }

        every { properties.chunkSize } returns 5

        runningStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns it
        }
        finishedStateAssets.forEach {
            coEvery { suspiciousItemsService.update(it) } returns it.copy(cursor = null)
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
}