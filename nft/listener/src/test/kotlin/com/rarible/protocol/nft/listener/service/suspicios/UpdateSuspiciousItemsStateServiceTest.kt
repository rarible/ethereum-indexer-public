package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.repository.JobStateRepository
import com.rarible.protocol.nft.listener.service.resolver.BluechipTokenResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class UpdateSuspiciousItemsStateServiceTest {
    private val bluechipTokenResolver = mockk<BluechipTokenResolver>()
    private val stateRepository = mockk<JobStateRepository>()
    private val service = UpdateSuspiciousItemsStateService(bluechipTokenResolver, stateRepository)

    @Test
    fun save() = runBlocking<Unit> {
        val state = randomUpdateSuspiciousItemsState()
        coEvery { stateRepository.save(any(), any()) } returns Unit
        service.save(state)
        coVerify {
            stateRepository.save(UpdateSuspiciousItemsState.STATE_ID, withArg<UpdateSuspiciousItemsState> {
                assertThat(it.copy(lastUpdatedAt = Instant.EPOCH)).isEqualTo(state.copy(lastUpdatedAt = Instant.EPOCH))
            })
        }
    }

    @Test
    fun get() = runBlocking<Unit> {
        val state = randomUpdateSuspiciousItemsState()
        coEvery { stateRepository.get(UpdateSuspiciousItemsState.STATE_ID, UpdateSuspiciousItemsState::class.java) } returns state
        val savedState = service.getState()
        assertThat(savedState).isEqualTo(state)
    }

    @Test
    fun getInit() = runBlocking<Unit> {
        val tokens = setOf(randomAddress(), randomAddress())
        val statedAt = Instant.now()
        every { bluechipTokenResolver.resolve() } returns tokens

        val initState = service.getInitState(statedAt)
        assertThat(initState.statedAt).isEqualTo(statedAt)
        assertThat(initState.assets).hasSize(2)
        assertThat(initState.assets.map { it.contract }).containsExactlyInAnyOrderElementsOf(tokens)
        assertThat(initState.assets.all { it.cursor == null }).isTrue
    }
}
