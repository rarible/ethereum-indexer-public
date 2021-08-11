package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo

@ChangeLog(order = "00011")
class ChangeLog00011UpdateTokeFeatures {

    @ChangeSet(id = "ChangeLog00011UpdateTokeFeatures.updateTokeFeatures", order = "1", author = "protocol")
    fun migrateTokenNonce(
        @NonLockGuarded protocol: ReactiveMongoTemplate,
        @NonLockGuarded tokenRepository: TokenRegistrationService
    ) = runBlocking<Unit> {
        logger.info("Token feature refresh start")

        val c = Token::features isEqualTo TokenFeature.MINT_AND_TRANSFER
        protocol.find(Query.query(c), Token::class.java).asFlow().collect {
            tokenRepository.updateFeatures(it).awaitFirst()
        }

        logger.info("Token feature refresh end")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00011UpdateTokeFeatures::class.java)
    }
}