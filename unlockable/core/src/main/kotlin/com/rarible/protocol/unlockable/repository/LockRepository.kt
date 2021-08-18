package com.rarible.protocol.unlockable.repository

import com.rarible.protocol.unlockable.domain.Lock
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component


@Component
class LockRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun save(lock: Lock): Lock {
        return mongo.save(lock).awaitSingle()
    }

    suspend fun findById(id: String): Lock? {
        return mongo.findById<Lock>(id).awaitFirstOrNull()
    }

    suspend fun findByItemId(itemId: String): Lock? {
        val c = Criteria.where(Lock::itemId.name).isEqualTo(itemId)
        return mongo
            .findOne<Lock>(
                Query(c).limit(1).with(Sort.by(Sort.Direction.DESC, "_id"))
            )
            .awaitFirstOrNull()
    }
}
