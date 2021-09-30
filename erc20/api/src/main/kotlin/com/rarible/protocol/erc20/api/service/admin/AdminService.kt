package com.rarible.protocol.erc20.api.service.admin

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.model.ReindexTokenTransferTaskParams
import com.rarible.protocol.erc20.core.model.ReindexTokenWithdrawalTaskParams
import com.rarible.protocol.erc20.core.repository.TempTaskRepository
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AdminService(
    private val taskRepository: TempTaskRepository
) {
    suspend fun createReindexTokenTask(token: Address, fromBlock: Long?): List<Task> {
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
}
