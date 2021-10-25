package com.rarible.protocol.nft.core.repository

import com.rarible.core.task.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import reactor.core.publisher.Mono

class TempTaskRepository(
    private val template: ReactiveMongoOperations
) {
    suspend fun save(task: Task): Task {
        return template.save(task).awaitFirst()
    }

    fun findByType(type: String): Flow<Task> {
        val criteria = Task::type isEqualTo type
        return template.find<Task>(Query.query(criteria)).asFlow()
    }

    fun findByTypeAndParam(type: String, param: String): Mono<Task> {
        val query = Query(
            Criteria().andOperator(
                Task::type isEqualTo type,
                Task::param isEqualTo param
            )
        )
        return template.findOne<Task>(query, Task::class.java)
    }
}
