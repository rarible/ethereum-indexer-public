package com.rarible.protocol.nft.core.service.action.executor

import com.rarible.core.common.nowMillis
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.nft.core.data.createRandomBurnItemAction
import com.rarible.protocol.nft.core.data.randomItemProperties
import com.rarible.protocol.nft.core.service.EnsDomainService
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.item.meta.descriptors.EnsDomainsPropertiesProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

internal class EnsDomainBurnActionExecutorTest {
    private val reducer = mockk<ItemReduceService>()
    private val ensDomainsPropertiesProvider = mockk<EnsDomainsPropertiesProvider>()
    private val ensDomainService = mockk<EnsDomainService>()

    private val executedBurnActionMetric = mockk<RegisteredCounter> {
        every { increment() } returns Unit
    }
    private val errorBurnActionMetric = mockk<RegisteredCounter>() {
        every { increment() } returns Unit
    }
    private val ensDomainBurnActionExecutor = EnsDomainBurnActionExecutor(
        reducer = reducer,
        ensDomainsPropertiesProvider = ensDomainsPropertiesProvider,
        ensDomainService = ensDomainService,
        executedBurnActionMetric = executedBurnActionMetric,
        errorBurnActionMetric = errorBurnActionMetric,
    )

    private companion object {

        @JvmStatic
        fun actionProperties(): Stream<Arguments> {
            val now = nowMillis()
            return Stream.of(
                Arguments.of(
                    // actionAt, expirationAt
                    now, now,
                ),
                Arguments.of(
                    // actionAt, expirationAt
                    now, now - Duration.ofHours(1)
                ),
                Arguments.of(
                    // actionAt, expirationAt
                    now, null
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("actionProperties")
    fun `should reduce item if time to burn`(actionAt: Instant, expirationAt: Instant?) = runBlocking<Unit> {
        val action = createRandomBurnItemAction().copy(actionAt = actionAt)
        val properties = randomItemProperties()

        coEvery { ensDomainsPropertiesProvider.get(action.itemId()) } returns properties
        every { reducer.update(token = action.token, tokenId = action.tokenId) } returns Flux.just(action.itemId())
        every { ensDomainService.getExpirationProperty(properties) } returns expirationAt

        ensDomainBurnActionExecutor.execute(action)

        verify { executedBurnActionMetric.increment() }
        coVerify { reducer.update(token = action.token, tokenId = action.tokenId) }
        coVerify(exactly = 0) { ensDomainService.onGetProperties(any(), any()) }
    }

    @Test
    fun `should not reduce item if time not come`() = runBlocking<Unit> {
        val actionAt = Instant.now()
        val action = createRandomBurnItemAction().copy(actionAt = actionAt)
        val properties = randomItemProperties()

        coEvery { ensDomainsPropertiesProvider.get(action.itemId()) } returns properties
        coEvery { ensDomainService.onGetProperties(action.itemId(), properties) } returns Unit
        every { ensDomainService.getExpirationProperty(properties) } returns actionAt + Duration.ofDays(1)

        ensDomainBurnActionExecutor.execute(action)

        verify(exactly = 0) { executedBurnActionMetric.increment() }
        coVerify(exactly = 0) { reducer.update(token = any(), tokenId = any()) }
        coVerify { ensDomainService.onGetProperties(action.itemId(), properties) }
    }

    @Test
    fun `should throw exception if can't get properties`() = runBlocking<Unit> {
        val action = createRandomBurnItemAction()

        coEvery { ensDomainsPropertiesProvider.get(action.itemId()) } returns null

        assertThrows<IllegalStateException> {
            runBlocking { ensDomainBurnActionExecutor.execute(action) }
        }
        verify { errorBurnActionMetric.increment() }

        verify(exactly = 0) { executedBurnActionMetric.increment() }
        coVerify(exactly = 0) { reducer.update(token = any(), tokenId = any()) }
        coVerify(exactly = 0) { ensDomainService.onGetProperties(any(), any()) }
    }
}
