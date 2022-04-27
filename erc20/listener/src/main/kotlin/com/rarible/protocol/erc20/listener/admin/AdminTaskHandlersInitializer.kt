package com.rarible.protocol.erc20.listener.admin

import com.rarible.core.task.TaskService
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.admin.model.ReduceErc20BalanceTaskParam
import com.rarible.protocol.erc20.core.admin.model.ReindexErc20TokenTaskParam
import com.rarible.protocol.erc20.core.admin.repository.Erc20TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!integration")
@FlowPreview
@ExperimentalCoroutinesApi
class AdminTaskHandlersInitializer(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val taskService: TaskService,
    private val taskRepository: Erc20TaskRepository
) {

    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    fun init() = runBlocking<Unit> {
        listOf(
            ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS,
            ReduceErc20BalanceTaskParam.ADMIN_BALANCE_REDUCE,
        ).forEach { type -> run(type) }
    }

    private suspend fun run(type: String) {
        taskRepository
            .findByType(type)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { taskService.runTask(type, it.param) }
    }
}
