package com.rarible.protocol.erc20.core.admin

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.ne
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class Erc20TaskRepository(
    private val template: ReactiveMongoOperations
) {

    suspend fun save(task: Task): Task {
        return template.save(task).awaitFirst()
    }

    fun findByTypeAndParam(type: String, param: String): Flow<Task> {
        val criteria = Criteria().andOperator(
            Task::type isEqualTo type,
            Task::param isEqualTo param
        )
        return template.find<Task>(Query.query(criteria)).asFlow()
    }

    fun findRunningByType(type: String): Flow<Task> {
        val criteria = Criteria().andOperator(
            Task::type isEqualTo type,
            Task::lastStatus ne TaskStatus.COMPLETED
        )
        return template.find<Task>(Query.query(criteria)).asFlow()
    }
}
