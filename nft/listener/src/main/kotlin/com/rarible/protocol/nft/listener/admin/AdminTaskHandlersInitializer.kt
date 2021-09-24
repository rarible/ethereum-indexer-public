package com.rarible.protocol.nft.listener.admin

import com.rarible.core.task.TaskService
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.model.ReindexTokenTaskParams.Companion.ADMIN_REINDEX_TOKEN
import com.rarible.protocol.nft.core.model.ReduceTokenItemsTaskParams.Companion.ADMIN_REDUCE_TOKEN_ITEMS
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ExperimentalCoroutinesApi
@FlowPreview
class AdminTaskHandlersInitializer(
    private val taskService: TaskService,
    private val taskRepository: TempTaskRepository
) {

    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() = runBlocking<Unit> {
        taskRepository
            .findByType(ADMIN_REINDEX_TOKEN)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(ADMIN_REINDEX_TOKEN, it.param) }

        taskRepository
            .findByType(ADMIN_REDUCE_TOKEN_ITEMS)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(ADMIN_REDUCE_TOKEN_ITEMS, it.param) }
    }
}
