package com.rarible.protocol.nft.core.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@CaptureSpan(type = SpanType.DB)
@Component
class JobStateRepository(
    private val template: ReactiveMongoTemplate,
    private val mapper: ObjectMapper,
) {

    suspend fun save(id: String, state: Any?) {
        val serialized = mapper.writeValueAsString(state)
        template.save(JobStateRecord(id, serialized), COLLECTION).awaitFirst()
    }

    suspend fun <T> get(id: String, clazz: Class<T>): T? {
        val state = template.findById(id, JobStateRecord::class.java, COLLECTION).awaitFirstOrNull()
        return if (state != null) {
            mapper.readValue(state.payload, clazz)
        } else {
            null
        }
    }

    data class JobStateRecord(
        val id: String,
        val payload: String?
    )

    private companion object {
        const val COLLECTION = "job_state"
    }
}
