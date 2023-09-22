package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenByteCode
import com.rarible.protocol.nft.core.repository.token.TokenByteCodeRepository
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "00029")
class ChangeLog00029TokenByteCodeScam {
    @ChangeSet(id = "ChangeLog00029TokenByteCodeScam.markScam", order = "1", author = "protocol")
    fun markScam(
        @NonLockGuarded template: ReactiveMongoOperations,
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties
    ) = runBlocking<Unit> {
        nftIndexerProperties.scamByteCodes.hashCodes.chunked(10).forEach { hashes ->
            template.updateMulti(
                Query.query(Criteria.where("_id").`in`(hashes)),
                Update.update("scam", true),
                TokenByteCode.COLLECTION
            ).awaitSingle()
        }
    }

    @ChangeSet(id = "ChangeLog00029TokenByteCodeScam.createIndexes", order = "1", author = "protocol")
    fun createIndexes(@NonLockGuarded template: ReactiveMongoOperations) = runBlocking<Unit> {
        TokenByteCodeRepository(template).createIndexes()
    }
}
