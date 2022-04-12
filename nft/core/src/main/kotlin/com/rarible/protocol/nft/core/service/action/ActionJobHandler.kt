package com.rarible.protocol.nft.core.service.action

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.service.action.executor.ActionExecutor
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ActionJobHandler(
    private val actionEventRepository: NftItemActionEventRepository,
    private val clock: Clock,
    actionExecutors: List<ActionExecutor<*>>
) : JobHandler {
    private val executors = actionExecutors.groupBy { it.type }

    override suspend fun handle() {
        val now = clock.instant()
        actionEventRepository.findPendingActions(now).collect { action ->
            val foundExecutors = requireNotNull(executors[action.type]) {
                "Can't find any action executors for ${action.type}, action=$action"
            }
            foundExecutors.forEach { executor ->
                @Suppress("UNCHECKED_CAST")
                (executor as ActionExecutor<Action>).execute(action)
            }
        }
    }
}