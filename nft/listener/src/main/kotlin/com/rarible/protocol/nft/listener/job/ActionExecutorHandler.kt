package com.rarible.protocol.nft.listener.job

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.Action
import com.rarible.protocol.nft.core.repository.action.NftItemActionEventRepository
import com.rarible.protocol.nft.core.service.action.ActionExecutor
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ActionExecutorHandler(
    private val actionEventRepository: NftItemActionEventRepository,
    private val clock: Clock,
    actionExecutors: List<ActionExecutor<Action>>
) : JobHandler {
    private val executors = actionExecutors.groupBy { it.type }

    override suspend fun handle() {
        val now = clock.instant()
        actionEventRepository.findPendingActions(now).collect { action ->
            val foundExecutors = requireNotNull(executors[action.type]) {
                "Can't find any action executors for ${action.type}, action=$action"
            }
            foundExecutors.forEach { executor -> executor.execute(action) }
        }
    }
}