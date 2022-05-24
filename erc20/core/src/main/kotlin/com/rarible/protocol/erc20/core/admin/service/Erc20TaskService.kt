package com.rarible.protocol.erc20.core.admin.service

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.admin.model.ReduceErc20BalanceTaskParam
import com.rarible.protocol.erc20.core.admin.model.ReindexErc20TokenTaskParam
import com.rarible.protocol.erc20.core.admin.repository.Erc20TaskRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class Erc20TaskService(
    private val taskRepository: Erc20TaskRepository
) {

    suspend fun createReindexErc20TokenTasks(tokens: List<Address>, fromBlock: Long?, force: Boolean): List<Task> {
        require(tokens.isNotEmpty()) { throw IllegalArgumentException("Erc20 tokens are not specified") }

        if (!force) checkRunningReindexErc20TokenTask(tokens)
        return ReindexErc20TokenTaskParam.Descriptor.values().map {
            saveTask(
                param = ReindexErc20TokenTaskParam(it, tokens).toParamString(),
                type = ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS,
                state = fromBlock,
                force = force
            )
        }
    }

    suspend fun createReduceErc20BalanceTask(token: Address, force: Boolean): Task {
        if (!force) checkRunningReduceErc20BalanceTask(token)
        return saveTask(
            param = ReduceErc20BalanceTaskParam(token).toParamString(),
            type = ReduceErc20BalanceTaskParam.ADMIN_BALANCE_REDUCE,
            state = null,
            force = force
        )
    }

    private suspend fun checkRunningReduceErc20BalanceTask(token: Address) {
        taskRepository
            .findByType(ReduceErc20BalanceTaskParam.ADMIN_BALANCE_REDUCE)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .firstOrNull()
            ?.let { throw IllegalArgumentException("Token $token reduce already in progress: $it") }
    }

    private suspend fun checkRunningReindexErc20TokenTask(tokens: List<Address>) {
        taskRepository
            .findByType(ReindexErc20TokenTaskParam.ADMIN_REINDEX_ERC20_TOKENS)
            .filter { it.lastStatus != TaskStatus.COMPLETED }
            .collect { task ->
                val param = ReindexErc20TokenTaskParam.fromParamString(task.param)
                val inProgress = param.tokens.filter { tokens.contains(it) }
                if (inProgress.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Tokens $inProgress are already being indexed in another task: $task"
                    )
                }
            }
    }

    private suspend fun saveTask(
        param: String,
        type: String,
        state: Any?,
        force: Boolean
    ): Task {
        return try {
            val newTask = if (force) {
                taskRepository.findByType(type, param).firstOrNull()?.copy(
                    state = state,
                    running = false,
                    lastStatus = TaskStatus.NONE
                )
            } else {
                null
            } ?: Task(
                type = type,
                param = param,
                state = state,
                running = false,
                lastStatus = TaskStatus.NONE
            )
            taskRepository.save(newTask)
        } catch (ex: Exception) {
            when (ex) {
                is OptimisticLockingFailureException, is DuplicateKeyException -> {
                    throw IllegalArgumentException("Reindex task already exists")
                }
                else -> throw ex
            }
        }
    }
}