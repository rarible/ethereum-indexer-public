package com.rarible.protocol.nft.core.repository

import com.rarible.core.task.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class TempTaskRepository(
    private val template: ReactiveMongoOperations
) {
    suspend fun save(task: Task): Task {
        return template.save(task).awaitFirst()
    }

    fun findByType(type: String, param: String? = null): Flow<Task> {
        val criteria = (Task::type isEqualTo type).let {
            if (param != null) {
                it.andOperator(Task::param isEqualTo param)
            } else {
                it
            }
        }
        return template.find<Task>(Query.query(criteria)).asFlow()
    }

    suspend fun findById(id: ObjectId): Task? {
        return template.findById<Task>(id).awaitFirstOrNull()
    }

    suspend fun countRunningTasks(type: String): Long {
        val criteria = Criteria().andOperator(
            Task::type isEqualTo type,
            Task::running isEqualTo true
        )
        return template.count<Task>(Query.query(criteria)).awaitFirst()
    }

    suspend fun delete(id: ObjectId) {
        val criteria = Criteria("_id").isEqualTo(id)
        template.remove(Query(criteria), Task::class.java).awaitFirstOrNull()
    }
}
