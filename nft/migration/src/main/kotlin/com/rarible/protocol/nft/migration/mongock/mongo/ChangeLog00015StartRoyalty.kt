package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations


@ChangeLog(order = "00015")
class ChangeLog00015StartRoyalty {

    @ChangeSet(id = "ChangeLog00015StartRoyalty.start", order = "1", author = "protocol")
    fun start(
        taskRepository: TaskRepository,
        mongo: ReactiveMongoOperations
    ) = runBlocking<Unit> {
        var task = taskRepository.findByTypeAndParam(TASK_NAME, "").awaitFirstOrNull()
        if (task == null) {
            task = Task(
                type = TASK_NAME,
                param = "",
                lastStatus = TaskStatus.NONE,
                state = null,
                running = false
            )
            mongo.save(task).awaitFirstOrNull()
            logger.info("Created $TASK_NAME task")
        }
    }

    companion object {
        const val TASK_NAME = "ROYALTY_REDUCE"
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00015StartRoyalty::class.java)
    }
}
