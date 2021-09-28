package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeLog(order = "00014")
class ChangeLog00015WipeUnknownItemNames {

    @ChangeSet(id = "ChangeLog00015WipeUnknownItemNames.run", order = "1", author = "protocol")
    fun run(mongo: ReactiveMongoOperations) = runBlocking<Unit> {
        val query = Query()
        query.addCriteria(
            Criteria().orOperator(
                Criteria.where("data.name").exists(false),
                Criteria.where("data.name").isEqualTo(""),
                Criteria.where("data.name").isEqualTo("Unknown"),
                Criteria.where("data.name").isEqualTo("Untitled")
            )
        )
        val result =  mongo.remove(query, "cache_opensea").awaitSingle()
        logger.info("Removed ${result.deletedCount} meta records from cache with unknown/empty names")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00015WipeUnknownItemNames::class.java)
    }
}
