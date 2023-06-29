package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.FeatureFlags
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Erc20EventServiceTest {
    private val erc20EventReduceService = mockk<Erc20EventReduceService>()
    private val erc20EventChainUpdateService = mockk<Erc20EventChainUpdateService>()
    private val featureFlags = mockk<FeatureFlags>()
    private val environmentInfo = ApplicationEnvironmentInfo("test", "local")

    private val service = Erc20EventService(
        erc20EventReduceService,
        erc20EventChainUpdateService,
        Erc20IndexerProperties(Blockchain.ETHEREUM, featureFlags = featureFlags),
        environmentInfo
    )

    @Test
    fun `use erc20EventReduceService`() = runBlocking<Unit> {
        val events = mockk<List<LogRecordEvent>>()

        every { featureFlags.chainBalanceUpdateEnabled } returns false
        coEvery { erc20EventReduceService.onEntityEvents(events) } returns Unit

        service.onEntityEvents(events)

        coVerify { erc20EventReduceService.onEntityEvents(events) }
        coVerify(exactly = 0) { erc20EventChainUpdateService.onEntityEvents(any()) }
    }

    @Test
    fun `use erc20EventChainUpdateService`() = runBlocking<Unit> {
        val events = mockk<List<LogRecordEvent>>()

        every { featureFlags.chainBalanceUpdateEnabled } returns true
        coEvery { erc20EventChainUpdateService.onEntityEvents(events) } returns Unit

        service.onEntityEvents(events)

        coVerify { erc20EventChainUpdateService.onEntityEvents(events) }
        coVerify(exactly = 0) { erc20EventReduceService.onEntityEvents(any()) }
    }
}