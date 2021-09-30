package com.rarible.protocol.erc20.listener.admin

import com.rarible.core.task.TaskService
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.model.ReindexTokenTransferTaskParams.Companion.ADMIN_REINDEX_TOKEN_TRANSFER
import com.rarible.protocol.erc20.core.model.ReindexTokenWithdrawalTaskParams.Companion.ADMIN_REINDEX_TOKEN_WITHDRAWAL
import com.rarible.protocol.erc20.core.repository.TempTaskRepository
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
    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    fun init() = runBlocking<Unit> {
        taskRepository
            .findByType(ADMIN_REINDEX_TOKEN_TRANSFER)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(ADMIN_REINDEX_TOKEN_TRANSFER, it.param) }

        taskRepository
            .findByType(ADMIN_REINDEX_TOKEN_WITHDRAWAL)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(ADMIN_REINDEX_TOKEN_WITHDRAWAL, it.param) }
    }
}
