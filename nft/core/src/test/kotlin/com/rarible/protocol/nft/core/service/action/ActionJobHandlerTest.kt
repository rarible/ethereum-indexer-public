package com.rarible.protocol.nft.core.service.action

import com.rarible.protocol.nft.core.data.createRandomBurnItemAction
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.model.ActionType
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.service.action.executor.ActionExecutor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class ActionJobHandlerTest {

    private val actionEventRepository = mockk<NftItemActionEventRepository>()
    private val executor1 = mockk<ActionExecutor<Action>> {
        every { type } returns ActionType.BURN
    }
    private val executor2 = mockk<ActionExecutor<Action>> {
        every { type } returns ActionType.BURN
    }
    private val jobHandler = ActionJobHandler(
        actionEventRepository = actionEventRepository,
        actionExecutors = listOf(executor1, executor2)
    )

    @Test
    fun `should execute actions`() = runBlocking<Unit> {
        val action = createRandomBurnItemAction().copy(state = ActionState.PENDING)

        every { actionEventRepository.findPendingActions(any()) } returns listOf(action).asFlow()
        every { actionEventRepository.save(any()) } answers { Mono.just(args.first() as Action) }
        listOf(executor1, executor2).forEach {
            coEvery { it.execute(action) } returns Unit
        }
        jobHandler.handle()
        verify { actionEventRepository.save(withArg {
            assertThat(it.id).isEqualTo(action.id)
            assertThat(it.state).isEqualTo(ActionState.EXECUTED)
        }) }
        coEvery { executor1.execute(action) }
        coEvery { executor2.execute(action) }
    }
}
