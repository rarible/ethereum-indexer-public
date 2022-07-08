package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.core.logging.Logger
import com.rarible.protocol.nft.core.model.TokenMetaContent
import com.rarible.protocol.nft.core.service.token.meta.TokenPropertiesService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.schema.JsonSchemaObject

@ChangeLog(order = "00023")
class ChangeLog00023TokenMetaContentFix {
    @ChangeSet(
        id = "ChangeLog00023TokenMetaContentFix",
        order = "1",
        author = "protocol"
    )
    fun fixData(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {
        val updateResult = template.updateMulti(
            Query(Criteria("data.properties.content").type(JsonSchemaObject.Type.arrayType())),
            Update().set("data.properties.content", TokenMetaContent()),
            TokenPropertiesService.TOKEN_METADATA_COLLECTION
        ).awaitSingle()

        logger.info("Fixed TokenMetaContent {}/{}", updateResult.modifiedCount, updateResult.matchedCount)
    }

    companion object {
        val logger by Logger()
    }
}