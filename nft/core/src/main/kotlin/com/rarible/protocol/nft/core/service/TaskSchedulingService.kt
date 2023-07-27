package com.rarible.protocol.nft.core.service

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class TaskSchedulingService(
    private val taskRepository: TempTaskRepository
) {
    suspend fun saveTask(
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
