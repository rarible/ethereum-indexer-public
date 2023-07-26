package com.rarible.protocol.erc20.core.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.blockchain.scanner.ethereum.task.EthereumReindexParam
import com.rarible.blockchain.scanner.reindex.BlockRange
import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.erc20.core.admin.model.Erc20DuplicatedLogRecordsTaskParam
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class Erc20TaskService(
    private val taskRepository: Erc20TaskRepository,
    private val mapper: ObjectMapper
) {

    suspend fun createReindexTask(
        tokens: List<Address>,
        fromBlock: Long,
        toBlock: Long?,
        force: Boolean
    ): Task {
        if (!force) checkRunningReindexErc20TokenTask(tokens)
        return saveTask(
            param = mapper.writeValueAsString(
                EthereumReindexParam(
                    range = BlockRange(fromBlock, toBlock, 250),
                    topics = emptyList(),
                    addresses = tokens
                )
            ),
            type = "BLOCK_SCANNER_REINDEX_TASK",
            state = fromBlock,
            force = force
        )
    }

    suspend fun createReduceTask(token: Address?, owner: Address?, force: Boolean): Task {
        val param = Erc20ReduceTaskParam(token, owner)
        if (!force) checkRunningReduceErc20BalanceTask(param)
        return saveTask(
            param = Erc20ReduceTaskParam(token, owner).toString(),
            type = Erc20ReduceTaskParam.TASK_TYPE,
            state = null,
            force = force
        )
    }

    suspend fun createDeduplicateTask(update: Boolean, force: Boolean): Task {
        val param = Erc20DuplicatedLogRecordsTaskParam(update = update)
        if (!force) checkRunningReduceErc20DeduplicateTask()
        return saveTask(
            param = mapper.writeValueAsString(param),
            type = Erc20DuplicatedLogRecordsTaskParam.ERC20_DUPLICATED_LOG_RECORDS_TASK,
            state = null,
            force = force
        )
    }

    private suspend fun checkRunningReduceErc20BalanceTask(param: Erc20ReduceTaskParam) {
        taskRepository
            .findRunningByType(Erc20ReduceTaskParam.TASK_TYPE)
            .filter { Erc20ReduceTaskParam.fromString(it.param).isOverlapped(param) }
            .firstOrNull()
            ?.let { throw IllegalArgumentException("Reduce for $param is overlapping running task: $it") }
    }

    private suspend fun checkRunningReduceErc20DeduplicateTask() {
        taskRepository
            .findRunningByType(Erc20DuplicatedLogRecordsTaskParam.ERC20_DUPLICATED_LOG_RECORDS_TASK)
            .firstOrNull()
            ?.let { throw IllegalArgumentException("Deduplicate erc20 log is already in progress") }
    }

    private suspend fun checkRunningReindexErc20TokenTask(tokens: List<Address>) {
        taskRepository
            .findRunningByType("BLOCK_SCANNER_REINDEX_TASK")
            .collect { task ->
                val param = mapper.readValue(task.param, EthereumReindexParam::class.java)
                val inProgress = param.addresses.filter { tokens.contains(it) }
                if (inProgress.isNotEmpty() || param.addresses.isEmpty()) {
                    // TODO ideally we should check block range overlap
                    throw IllegalArgumentException(("Reindex for $param is overlapping running task: $task"))
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
                taskRepository.findByTypeAndParam(type, param).firstOrNull()?.copy(
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
