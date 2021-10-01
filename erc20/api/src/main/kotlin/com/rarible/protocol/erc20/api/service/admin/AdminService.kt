package com.rarible.protocol.erc20.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.model.ReduceTokenTaskParams
import com.rarible.protocol.erc20.core.model.TokenTaskParams
import com.rarible.protocol.erc20.core.model.ReindexTokenTransferTaskParams
import com.rarible.protocol.erc20.core.model.ReindexTokenWithdrawalTaskParams
import com.rarible.protocol.erc20.core.repository.TempTaskRepository
import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AdminService(
    private val taskRepository: TempTaskRepository
) {
    suspend fun createReindexTokenTask(token: Address, fromBlock: Long): List<Task> {
        checkOtherTokenTasks(ReindexTokenTransferTaskParams.ADMIN_REINDEX_TOKEN_TRANSFER, token)
        checkOtherTokenTasks(ReindexTokenWithdrawalTaskParams.ADMIN_REINDEX_TOKEN_WITHDRAWAL, token)

        val transferTask = Task(
            type = ReindexTokenTransferTaskParams.ADMIN_REINDEX_TOKEN_TRANSFER,
            param = ReindexTokenTransferTaskParams(token).toParamString(),
            state = fromBlock,
            running = false,
            lastStatus = TaskStatus.NONE
        )
        val savedTransferTask = taskRepository.save(transferTask)

        val withdrawalTask = Task(
            type = ReindexTokenWithdrawalTaskParams.ADMIN_REINDEX_TOKEN_WITHDRAWAL,
            param = ReindexTokenWithdrawalTaskParams(token).toParamString(),
            state = fromBlock,
            running = false,
            lastStatus = TaskStatus.NONE
        )
        val savedWithdrawalTask = taskRepository.save(withdrawalTask)

        return listOf(savedTransferTask, savedWithdrawalTask)
    }

    suspend fun createReduceTokenTask(token: Address): List<Task> {
        checkOtherTokenTasks(ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN, token)

        val reduceTask = Task(
            type = ReduceTokenTaskParams.ADMIN_REDUCE_TOKEN,
            param = ReduceTokenTaskParams(token).toParamString(),
            state = null,
            running = false,
            lastStatus = TaskStatus.NONE
        )
        val savedReduceTask = taskRepository.save(reduceTask)

        return listOf(savedReduceTask)
    }

    private suspend fun checkOtherTokenTasks(type: String, token: Address) {
        taskRepository.findByType(type).collect { task ->
            if (task.lastStatus != TaskStatus.COMPLETED) {
                val existedToken = TokenTaskParams.fromParamString(task.param)

                if (existedToken == token) {
                    throw IllegalArgumentException("Token $token task $type (${task.id}) still running")
                }
            }
        }
    }
}
