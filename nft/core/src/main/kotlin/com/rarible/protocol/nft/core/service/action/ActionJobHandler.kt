package com.rarible.protocol.nft.core.service.action

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.service.action.executor.ActionExecutor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ActionJobHandler(
    private val actionEventRepository: NftItemActionEventRepository,
    actionExecutors: List<ActionExecutor<*>>
) : JobHandler {
    private val executors = actionExecutors.groupBy { it.type }

    override suspend fun handle() {
        val now = Instant.now()
        actionEventRepository.findPendingActions(now).collect { action ->
            val foundExecutors = requireNotNull(executors[action.type]) {
                "Can't find any action executors for ${action.type}, action=$action"
            }
            foundExecutors.forEach { executor ->
                @Suppress("UNCHECKED_CAST")
                (executor as ActionExecutor<Action>).execute(action)
            }
            actionEventRepository.save(action.withState(ActionState.EXECUTED)).awaitFirst()
        }
    }
}
