package com.rarible.protocol.nft.listener.admin

import com.mongodb.client.model.Filters
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.stereotype.Component

/**
 * RPN-1316: background job that iterates over all LogEntry-s in the database,
 * tries to deserialize them and logs an error if failed.
 */
@Component
class FindNonParseableLogEntriesTaskHandler(
    private val mongo: ReactiveMongoOperations,
    private val mongoConverter: MongoConverter
) : TaskHandler<String> {

    override val type: String
        get() = "FIND_NON_PARSEABLE_LOG_ENTRIES"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        logger.info("Started finding non-parseable log entries from ${from ?: "the first"} log")
        val emptyCallback = object : OnNonParseableEntryCallback {
            override fun onEntryFound(objectId: ObjectId) = Unit
        }
        return findNonParseableLogEntries(
            from?.let { ObjectId(it) },
            emptyCallback,
            ItemHistory::class.java,
            NftItemHistoryRepository.COLLECTION
        )
    }

    fun <T> findNonParseableLogEntries(
        fromId: ObjectId?,
        callback: OnNonParseableEntryCallback,
        targetClass: Class<T>,
        collectionName: String
    ): Flow<String> {
        val collection = mongo.getCollection(collectionName).block()!!
        val documentsFlow = if (fromId != null) {
            collection.find(Filters.gt("_id", fromId))
        } else {
            collection.find()
        }.asFlow()

        var totalProcessed = 0
        return documentsFlow
            .map { document ->
                val objectId = document.getObjectId("_id")
                checkNotNull(objectId) { "Invalid document $document" }
                if (!checkCanBeParsed(document, targetClass)) {
                    callback.onEntryFound(objectId)
                }
                objectId.toString()
            }
            .onEach {
                if (++totalProcessed % 100000 == 0) {
                    logger.info("Non-parseable log entries: total processed $totalProcessed log entries")
                }
            }
    }

    private fun <T> checkCanBeParsed(document: Document, targetClass: Class<T>): Boolean {
        val objectId = document.getObjectId("_id")
        val rawData = document["data"] as? Bson
        if (rawData == null) {
            logger.warn("No 'data' field found in $objectId: $document")
            return false
        }
        val eventData = try {
            mongoConverter.read(targetClass, rawData)
        } catch (e: Exception) {
            logger.warn("Found non-parseable log entry: $objectId: ${e.message}")
            return false
        }
        if (eventData == null) {
            logger.warn("No 'data' field found in $objectId: $document")
            return false
        }
        return true
    }

    interface OnNonParseableEntryCallback {
        fun onEntryFound(objectId: ObjectId)
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(FindNonParseableLogEntriesTaskHandler::class.java)
    }

}
